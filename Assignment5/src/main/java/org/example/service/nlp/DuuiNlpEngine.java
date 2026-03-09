package org.example.service.nlp;

/**
 * Developer guide: Assignment4-compatible DUUI engine via single HTTP JSON endpoint.
 */

import org.example.config.NlpConfig;
import org.example.model.Speech;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DUUI NLP engine that forwards speech text to one HTTP endpoint and maps JSON response fields.
 */
public class DuuiNlpEngine implements NlpEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DuuiNlpEngine.class);

    private final HttpClient httpClient;
    private final NlpConfig config;

    /**
     * Creates an Assignment4-style DUUI engine.
     */
    public DuuiNlpEngine(NlpConfig config) {
        this.httpClient = HttpClient.newHttpClient();
        this.config = config;
    }

    /**
     * Engine identifier used in logs and metadata.
     */
    @Override
    public String name() {
        return "duui";
    }

    /**
     * Engine is available if an explicit DUUI endpoint is set or (fallback) spaCy target is set.
     */
    @Override
    public boolean isAvailable() {
        return config != null && resolveEndpoint() != null;
    }

    /**
     * Calls DUUI endpoint with speech text and maps response into speech NLP fields.
     */
    @Override
    public void annotate(Speech speech) {
        String endpoint = resolveEndpoint();
        if (endpoint == null) {
            throw new IllegalStateException("DUUI endpoint is not configured");
        }

        JSONObject payload = buildPayload(speech);
        try {
            HttpResponse<String> response = send(endpoint, payload.toString());
            if (response.statusCode() == 404 && !endpoint.endsWith("/v1/process")) {
                response = send(endpoint + "/v1/process", payload.toString());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DUUI request failed: HTTP " + response.statusCode() + " body=" + response.body());
            }

            JSONObject parsed;
            try {
                parsed = new JSONObject(response.body());
            } catch (RuntimeException ex) {
                String body = response.body() == null ? "" : response.body();
                String preview = body.length() > 300 ? body.substring(0, 300) + "..." : body;
                throw new IllegalStateException("DUUI response is not valid JSON: " + preview, ex);
            }
            Map<String, Object> nlp = parsed.toMap();
            nlp.putIfAbsent("sourceFormat", "duui-json");
            speech.setNlp(nlp);
            mapTopLevelFields(speech, parsed, nlp);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("DUUI request failed: " + e.getMessage(), e);
        }
    }

    private JSONObject buildPayload(Speech speech) {
        JSONObject payload = new JSONObject();
        payload.put("speechId", speech.getId() == null ? "" : speech.getId());
        payload.put("text", speech.getText() == null ? "" : speech.getText());
        // DUUI spaCy endpoint requires `lang`; without it we receive HTTP 422.
        payload.put("lang", notBlank(config.duuiSpacyLanguage()) ? config.duuiSpacyLanguage() : "de");
        if (notBlank(config.duuiSelection())) {
            payload.put("selection", config.duuiSelection());
        }
        if (notBlank(config.duuiViewName())) {
            payload.put("view", config.duuiViewName());
        }
        return payload;
    }

    private HttpResponse<String> send(String endpoint, String body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(Math.max(1, config.duuiTimeoutSeconds())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (config.hasDuuiToken()) {
            builder.header("Authorization", "Bearer " + config.duuiAuthToken());
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String resolveEndpoint() {
        if (config == null) {
            return null;
        }
        if (config.hasDuuiEndpoint()) {
            return config.duuiEndpoint();
        }
        String fallback = config.duuiSpacyTarget();
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private void mapTopLevelFields(Speech speech, JSONObject parsed, Map<String, Object> nlp) {
        List<Object> topics = valueAsObjectList(parsed.opt("topics"));
        List<Object> namedEntities = valueAsObjectList(parsed.opt("namedEntities"));
        if (namedEntities.isEmpty()) {
            namedEntities = valueAsObjectList(parsed.opt("entities"));
        }
        Map<String, Integer> posDistribution = valueAsIntMap(parsed.optJSONObject("posDistribution"));
        if (posDistribution.isEmpty()) {
            posDistribution = inferPosDistributionFromTokens(parsed.opt("tokens"));
        }

        List<Map<String, Object>> sentenceRows = valueAsSentenceRows(parsed.opt("sentenceSentiments"));
        if (sentenceRows.isEmpty()) {
            sentenceRows = buildNeutralSentenceRows(parsed.opt("sentences"), speech.getText());
        }
        List<Double> sentenceValues = valueAsDoubleList(parsed.opt("sentenceSentiments"));
        if (sentenceValues.isEmpty()) {
            sentenceValues = extractSentenceScores(sentenceRows);
        }
        List<Double> sentiments = valueAsDoubleList(parsed.opt("sentiments"));
        if (sentiments.isEmpty()) {
            sentiments = new ArrayList<>(sentenceValues);
        }

        speech.setTopics(topics);
        speech.setNamedEntities(namedEntities);
        speech.setPosDistribution(posDistribution);
        speech.setSentenceSentiments(sentenceValues);
        speech.setSentiments(sentiments);

        nlp.put("topics", topics);
        nlp.put("namedEntities", namedEntities);
        nlp.put("posDistribution", posDistribution);
        if (!sentenceRows.isEmpty()) {
            nlp.put("sentenceSentiments", sentenceRows);
        }
        if (!sentiments.isEmpty()) {
            nlp.put("sentiments", sentiments);
        }
        LOGGER.debug("DUUI normalized payload for speech {}: topics={}, entities={}, sentenceRows={}",
                speech.getId(), topics.size(), namedEntities.size(), sentenceRows.size());
    }

    private List<Map<String, Object>> valueAsSentenceRows(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                Object row = array.get(i);
                if (row instanceof JSONObject jsonRow) {
                    out.add(jsonRow.toMap() instanceof Map<?, ?> m ? castObjectMap(m) : new HashMap<>());
                } else if (row instanceof Map<?, ?> map) {
                    out.add(castObjectMap(map));
                } else if (row instanceof Number n) {
                    out.add(new HashMap<>(Map.of("score", n.doubleValue())));
                }
            }
            return out;
        }
        if (value instanceof List<?> list) {
            for (Object row : list) {
                if (row instanceof Map<?, ?> map) {
                    out.add(castObjectMap(map));
                } else if (row instanceof Number n) {
                    out.add(new HashMap<>(Map.of("score", n.doubleValue())));
                }
            }
        }
        return out;
    }

    private List<Map<String, Object>> buildNeutralSentenceRows(Object sentencesValue, String text) {
        List<Map<String, Object>> out = new ArrayList<>();
        List<Object> sentences = valueAsObjectList(sentencesValue);
        for (Object row : sentences) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Integer begin = asInt(map.get("begin"));
            Integer end = asInt(map.get("end"));
            if (begin == null || end == null || begin < 0 || end <= begin) {
                continue;
            }
            Map<String, Object> sentimentRow = new HashMap<>();
            sentimentRow.put("begin", begin);
            sentimentRow.put("end", end);
            sentimentRow.put("score", 0.0);
            if (text != null && end <= text.length()) {
                sentimentRow.put("sentence", text.substring(begin, end));
            }
            out.add(sentimentRow);
        }
        return out;
    }

    private List<Double> extractSentenceScores(List<Map<String, Object>> rows) {
        List<Double> out = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object score = row.get("score");
            if (score instanceof Number n) {
                out.add(n.doubleValue());
            }
        }
        return out;
    }

    private Map<String, Integer> inferPosDistributionFromTokens(Object tokensValue) {
        Map<String, Integer> out = new HashMap<>();
        List<Object> tokens = valueAsObjectList(tokensValue);
        for (Object token : tokens) {
            if (!(token instanceof Map<?, ?> map)) {
                continue;
            }
            String coarse = asString(map.get("pos_coarse"));
            String fine = asString(map.get("pos"));
            String key = notBlank(coarse) ? coarse : (notBlank(fine) ? fine : "");
            if (!key.isBlank()) {
                out.put(key, out.getOrDefault(key, 0) + 1);
            }
        }
        return out;
    }

    private Map<String, Object> castObjectMap(Map<?, ?> input) {
        Map<String, Object> out = new HashMap<>();
        for (Map.Entry<?, ?> entry : input.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }

    private Integer asInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private List<Object> valueAsObjectList(Object value) {
        if (value instanceof JSONArray array) {
            return array.toList();
        }
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return new ArrayList<>();
    }

    private List<Double> valueAsDoubleList(Object value) {
        List<Double> out = new ArrayList<>();
        if (value instanceof JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                Object item = array.get(i);
                if (item instanceof Number n) {
                    out.add(n.doubleValue());
                }
            }
            return out;
        }
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number n) {
                    out.add(n.doubleValue());
                }
            }
        }
        return out;
    }

    private Map<String, Integer> valueAsIntMap(JSONObject object) {
        Map<String, Integer> out = new HashMap<>();
        if (object == null) {
            return out;
        }
        for (String key : object.keySet()) {
            Object value = object.get(key);
            if (value instanceof Number n) {
                out.put(key, n.intValue());
            }
        }
        return out;
    }
}
