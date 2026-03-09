package org.example.service.nlp;

/**
 * Developer guide: Builds compact UIMA-like snapshot payload from speech and NLP fields.
 */

import org.example.model.Comment;
import org.example.model.Speech;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a compact UIMA-like serialization payload from a speech and its NLP fields.
 */
public final class UimaCasSerializer {
    private UimaCasSerializer() {
    }

    /**
     * Enriches the speech NLP map with `uimaTypeSystem`, `uimaCas` and serialization timestamp fields.
     */
    public static void enrichSpeechWithUimaCas(Speech speech) {
        Map<String, Object> nlp = speech.getNlp();
        if (nlp == null) {
            nlp = new LinkedHashMap<>();
            speech.setNlp(nlp);
        }
        nlp.put("uimaTypeSystem", buildTypeSystem());
        nlp.put("uimaCas", buildCas(speech));
        nlp.put("uimaSerializedAt", Instant.now().toString());
    }

    private static Map<String, Object> buildTypeSystem() {
        List<Map<String, Object>> types = new ArrayList<>();
        types.add(type("ppr.Speech", List.of("speechId", "protocolId", "sessionId")));
        types.add(type("ppr.Topic", List.of("label", "score")));
        types.add(type("de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS", List.of("tag", "count")));
        types.add(type("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity", List.of("value", "entityType", "begin", "end")));
        types.add(type("ppr.SentenceSentiment", List.of("score", "begin", "end")));
        types.add(type("ppr.SentenceSarcasm", List.of("score", "begin", "end")));
        types.add(type("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference", List.of("chain", "begin", "end")));
        types.add(type("ppr.Comment", List.of("author", "faction", "speechOffset", "text")));
        return new LinkedHashMap<>(Map.of(
                "name", "ppr.multimodal.typesystem.v1",
                "types", types
        ));
    }

    private static Map<String, Object> type(String name, List<String> features) {
        return new LinkedHashMap<>(Map.of(
                "name", name,
                "features", features
        ));
    }

    private static Map<String, Object> buildCas(Speech speech) {
        List<Map<String, Object>> annotations = new ArrayList<>();
        addSentenceSentimentAnnotations(annotations, speech);
        addSentenceSarcasmAnnotations(annotations, speech);
        addNamedEntityAnnotations(annotations, speech);
        addTopicAnnotations(annotations, speech);
        addPosAnnotations(annotations, speech);
        addCoreferenceAnnotations(annotations, speech);
        addCommentAnnotations(annotations, speech);

        Map<String, Object> cas = new LinkedHashMap<>();
        cas.put("documentText", speech.getText() == null ? "" : speech.getText());
        cas.put("language", "de");
        cas.put("speechId", speech.getId());
        cas.put("protocolId", speech.getProtocolId());
        cas.put("sessionId", speech.getSessionId());
        cas.put("annotations", annotations);
        return cas;
    }

    @SuppressWarnings("unchecked")
    private static void addSentenceSentimentAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        Object value = speech.getNlp() == null ? null : speech.getNlp().get("sentenceSentiments");
        if (!(value instanceof List<?> rows)) {
            return;
        }
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Integer begin = asInt(map.get("begin"));
            Integer end = asInt(map.get("end"));
            Double score = asDouble(map.get("score"));
            if (begin == null || end == null || score == null) {
                continue;
            }
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "ppr.SentenceSentiment");
            ann.put("begin", begin);
            ann.put("end", end);
            ann.put("score", score);
            annotations.add(ann);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addSentenceSarcasmAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        Object value = speech.getNlp() == null ? null : speech.getNlp().get("sentenceSarcasm");
        if (!(value instanceof List<?> rows)) {
            return;
        }
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) {
                continue;
            }
            Integer begin = asInt(map.get("begin"));
            Integer end = asInt(map.get("end"));
            Double score = asDouble(map.get("score"));
            if (begin == null || end == null || score == null) {
                continue;
            }
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "ppr.SentenceSarcasm");
            ann.put("begin", begin);
            ann.put("end", end);
            ann.put("score", score);
            annotations.add(ann);
        }
    }

    private static void addNamedEntityAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        if (speech.getNamedEntities() == null) {
            return;
        }
        for (Object entity : speech.getNamedEntities()) {
            if (!(entity instanceof Map<?, ?> map)) {
                continue;
            }
            Integer begin = asInt(map.get("begin"));
            Integer end = asInt(map.get("end"));
            if (begin == null || end == null) {
                continue;
            }
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity");
            ann.put("begin", begin);
            ann.put("end", end);
            Object textValue = map.get("text");
            Object typeValue = map.get("type");
            ann.put("value", textValue == null ? "" : String.valueOf(textValue));
            ann.put("entityType", typeValue == null ? "UNKNOWN" : String.valueOf(typeValue));
            annotations.add(ann);
        }
    }

    private static void addTopicAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        if (speech.getTopics() == null) {
            return;
        }
        for (Object topic : speech.getTopics()) {
            if (!(topic instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "ppr.Topic");
            Object label = map.get("label");
            if (label == null) {
                label = map.get("topic");
            }
            ann.put("label", label == null ? "" : String.valueOf(label));
            ann.put("score", asDouble(map.get("score")) == null ? 0.0 : asDouble(map.get("score")));
            annotations.add(ann);
        }
    }

    private static void addPosAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        if (speech.getPosDistribution() == null) {
            return;
        }
        for (Map.Entry<String, Integer> e : speech.getPosDistribution().entrySet()) {
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS");
            ann.put("tag", e.getKey());
            ann.put("count", e.getValue());
            annotations.add(ann);
        }
    }

    @SuppressWarnings("unchecked")
    private static void addCoreferenceAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        Object value = speech.getNlp() == null ? null : speech.getNlp().get("coreferences");
        if (!(value instanceof List<?> groups)) {
            return;
        }
        for (Object group : groups) {
            if (!(group instanceof Map<?, ?> groupMap)) {
                continue;
            }
            Object chainValue = groupMap.get("label");
            String chain = chainValue == null ? "coref" : String.valueOf(chainValue);
            Object mentionsValue = groupMap.get("mentions");
            if (!(mentionsValue instanceof List<?> mentions)) {
                continue;
            }
            for (Object mention : mentions) {
                if (!(mention instanceof Map<?, ?> mentionMap)) {
                    continue;
                }
                Integer begin = asInt(mentionMap.get("begin"));
                Integer end = asInt(mentionMap.get("end"));
                if (begin == null || end == null) {
                    continue;
                }
                Map<String, Object> ann = new LinkedHashMap<>();
                ann.put("type", "de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
                ann.put("begin", begin);
                ann.put("end", end);
                ann.put("chain", chain);
                annotations.add(ann);
            }
        }
    }

    private static void addCommentAnnotations(List<Map<String, Object>> annotations, Speech speech) {
        if (speech.getComments() == null) {
            return;
        }
        for (Comment comment : speech.getComments()) {
            Map<String, Object> ann = new LinkedHashMap<>();
            ann.put("type", "ppr.Comment");
            ann.put("author", nullToEmpty(comment.getAuthorName()));
            ann.put("faction", nullToEmpty(comment.getAuthorFaction()));
            ann.put("speechOffset", comment.getSpeechOffset());
            ann.put("text", nullToEmpty(comment.getText()));
            annotations.add(ann);
        }
    }

    private static Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
