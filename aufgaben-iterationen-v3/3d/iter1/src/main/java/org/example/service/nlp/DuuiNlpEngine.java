package org.example.service.nlp;

import org.example.config.NlpConfig;
import org.example.model.Speech;
import org.json.JSONArray;
import org.json.JSONObject;

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
 * @author
 * Victor Weniger
 */

/**
 * DuuiNlpEngine service
 */
public class DuuiNlpEngine implements NlpEngine {
    private final HttpClient httpClient;
    private final NlpConfig config;

/**
 * Constructor
 */
    public DuuiNlpEngine(NlpConfig config) {
        this.httpClient = HttpClient.newHttpClient();
        this.config = config;
    }

    @Override

/**
 * Method
 */
    public String name() {
        return "duui";
    }

    @Override

/**
 * Getter
 */
    public boolean isAvailable() {
        return config != null && resolveEndpoint() != null;
    }

    @Override

/**
 * Method
 */
    public void annotate(Speech speech) {
        String endpoint = resolveEndpoint();
        if (endpoint == null) {
            throw new IllegalStateException("DUUI endpoint is not configured");
        }

        JSONObject payload = new JSONObject();
        payload.put("speechId", speech.getId() == null ? "" : speech.getId());
        payload.put("text", speech.getText() == null ? "" : speech.getText());
        try {
            HttpResponse<String> response = send(endpoint, payload.toString());
            if (response.statusCode() == 404 && !endpoint.endsWith("/v1/process")) {
                response = send(endpoint + "/v1/process", payload.toString());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("DUUI request failed: HTTP " + response.statusCode() + " body=" + response.body());
            }

            JSONObject parsed = new JSONObject(response.body());
            Map<String, Object> nlp = parsed.toMap();
            nlp.putIfAbsent("sourceFormat", "duui-json");
            speech.setNlp(nlp);
            mapTopLevelFields(speech, parsed);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("DUUI request failed: " + e.getMessage(), e);
        }
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

    private void mapTopLevelFields(Speech speech, JSONObject parsed) {
        speech.setTopics(valueAsObjectList(parsed.opt("topics")));
        speech.setNamedEntities(valueAsObjectList(parsed.opt("namedEntities")));
        speech.setPosDistribution(valueAsIntMap(parsed.optJSONObject("posDistribution")));
        speech.setSentenceSentiments(valueAsDoubleList(parsed.opt("sentenceSentiments")));
        speech.setSentiments(valueAsDoubleList(parsed.opt("sentiments")));
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
