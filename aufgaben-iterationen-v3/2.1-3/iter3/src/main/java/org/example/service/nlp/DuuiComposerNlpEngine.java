package org.example.service.nlp;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.example.config.NlpConfig;
import org.example.model.Speech;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author
 * Victor Weniger
 */

/**
 * DuuiComposerNlpEngine service
 */
public class DuuiComposerNlpEngine implements NlpEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(DuuiComposerNlpEngine.class);

    private static final String T_SENTENCE = "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence";
    private static final String T_TOKEN = "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token";
    private static final String T_NER = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";
    private static final String T_TOPIC_CAT = "org.texttechnologylab.annotation.semaf.isobase.CategoryCoveredTagged";
    private static final List<String> SENTIMENT_TYPE_CANDIDATES = List.of(
            "org.hucompute.textimager.uima.type.GerVaderSentiment",
            "org.hucompute.textimager.uima.type.Sentiment",
            "org.hucompute.textimager.uima.type.VaderSentiment",
            "org.texttechnologylab.annotation.type.Sentiment"
    );
    private static final List<String> TOPIC_TYPE_CANDIDATES = List.of(
            "org.texttechnologylab.annotation.topics.Topic",
            "org.texttechnologylab.annotation.topic.Topic",
            "org.hucompute.textimager.uima.type.topic.Topic"
    );

    private final NlpConfig config;
    private volatile DuuiComposerPipeline pipeline;

/**
 * Constructor
 */
    public DuuiComposerNlpEngine(NlpConfig config) {
        this.config = config;
    }

    @Override

/**
 * Method
 */
    public String name() {
        return "duui-composer";
    }

    @Override

/**
 * Getter
 */
    public boolean isAvailable() {
        return config != null && config.hasDuuiPipelineTargets();
    }

    @Override

/**
 * Method
 */
    public void annotate(Speech speech) {
        if (speech == null) {
            throw new IllegalArgumentException("speech must not be null");
        }
        if (!isAvailable()) {
            throw new IllegalStateException("DUUI composer requires spacy/gervader/parlbert targets");
        }
        try {
            DuuiComposerPipeline duui = ensurePipeline();
            String viewName = duui.viewName();
            JCas base = buildInputCas(speech, viewName);
            JCas selectedView = resolveView(base, viewName);
            duui.process(selectedView);

            JCas nlpView = resolveView(base, viewName);
            ParsedNlp parsed = parseFromCas(base, nlpView, speech.getText());

            speech.setTopics(new ArrayList<>(parsed.topics));
            speech.setNamedEntities(new ArrayList<>(parsed.namedEntities));
            speech.setPosDistribution(new HashMap<>(parsed.posDistribution));
            speech.setSentenceSentiments(new ArrayList<>(parsed.sentenceScores));
            speech.setSentiments(new ArrayList<>(parsed.sentiments));

            Map<String, Object> nlp = speech.getNlp() == null ? new HashMap<>() : speech.getNlp();
            nlp.put("sourceFormat", "duui-composer-cas");
            nlp.put("duuiSelection", duui.selection());
            nlp.put("duuiView", viewName);
            nlp.put("topics", parsed.topics);
            nlp.put("namedEntities", parsed.namedEntities);
            nlp.put("posDistribution", parsed.posDistribution);
            nlp.put("sentenceSentiments", parsed.sentenceRows);
            nlp.put("sentiments", parsed.sentiments);
            nlp.put("sentenceSarcasm", parsed.sarcasmRows);
            speech.setNlp(nlp);

            LOGGER.debug("DUUI composer NLP for speech {}: topics={}, entities={}, sentiments={}, sarcasm={}",
                    speech.getId(),
                    parsed.topics.size(),
                    parsed.namedEntities.size(),
                    parsed.sentenceRows.size(),
                    parsed.sarcasmRows.size());
        } catch (Exception ex) {
            throw new IllegalStateException("DUUI composer NLP failed: " + summarizeException(ex), ex);
        }
    }

    private static String summarizeException(Throwable throwable) {
        StringBuilder out = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            String message = String.valueOf(current.getMessage());
            if (depth > 0) {
                out.append(" | caused by: ");
            }
            out.append(current.getClass().getSimpleName());
            if (message != null && !message.isBlank() && !"null".equals(message)) {
                out.append(": ").append(message);
            }
            current = current.getCause();
            depth++;
        }
        return out.toString();
    }

    private DuuiComposerPipeline ensurePipeline() {
        if (pipeline != null) {
            return pipeline;
        }
        synchronized (this) {
            if (pipeline == null) {
                pipeline = DuuiComposerPipeline.create(config);
            }
            return pipeline;
        }
    }

    private JCas buildInputCas(Speech speech, String viewName) throws Exception {
        JCas base = org.apache.uima.fit.factory.JCasFactory.createJCas();
        String text = speech.getText() == null ? "" : speech.getText();
        base.setDocumentLanguage("de");
        base.setDocumentText(text);
        JCas selectedView = resolveView(base, viewName);
        selectedView.setDocumentLanguage("de");
        selectedView.setDocumentText(text);
        return base;
    }

    private JCas resolveView(JCas base, String viewName) throws Exception {
        if (viewName == null || viewName.isBlank() || "_InitialView".equals(viewName)) {
            return base;
        }
        try {
            return base.getView(viewName);
        } catch (Exception ignored) {
            return base.createView(viewName);
        }
    }

    private ParsedNlp parseFromCas(JCas base, JCas nlpView, String fallbackText) {
        List<AnnotationFS> sentenceAnns = selectByTypeName(nlpView, T_SENTENCE);
        sentenceAnns.sort(Comparator.comparingInt(AnnotationFS::getBegin));

        List<Map<String, Object>> sentenceRows = new ArrayList<>();
        List<Double> sentenceScores = new ArrayList<>();
        List<Double> sentiments = new ArrayList<>();
        List<Map<String, Object>> sarcasmRows = new ArrayList<>();

        for (AnnotationFS sentence : sentenceAnns) {
            int begin = sentence.getBegin();
            int end = sentence.getEnd();
            if (begin < 0 || end <= begin) {
                continue;
            }
            String sentenceText = safeSpan(nlpView.getDocumentText(), fallbackText, begin, end);
            double score = meanSentimentForSpan(nlpView, begin, end);
            sentenceRows.add(new HashMap<>(Map.of(
                    "begin", begin,
                    "end", end,
                    "sentence", sentenceText,
                    "score", score
            )));
            sentenceScores.add(score);
            sentiments.add(score);
        }

        sarcasmRows.addAll(extractSarcasmRows(nlpView, sentenceRows));

        List<Object> namedEntities = new ArrayList<>();
        for (AnnotationFS ne : selectByTypeName(nlpView, T_NER)) {
            int begin = ne.getBegin();
            int end = ne.getEnd();
            if (begin < 0 || end <= begin) {
                continue;
            }
            String label = firstStringFeature(ne, "value", "Value", "entityType", "type", "Type");
            String text = safeSpan(nlpView.getDocumentText(), fallbackText, begin, end);
            Map<String, Object> row = new HashMap<>();
            row.put("begin", begin);
            row.put("end", end);
            row.put("text", text);
            row.put("type", label == null || label.isBlank() ? "UNKNOWN" : label);
            namedEntities.add(row);
        }

        Map<String, Integer> posDistribution = new HashMap<>();
        for (AnnotationFS token : selectByTypeName(nlpView, T_TOKEN)) {
            String pos = readPosTag(token);
            if (pos == null || pos.isBlank()) {
                continue;
            }
            posDistribution.put(pos, posDistribution.getOrDefault(pos, 0) + 1);
        }

        List<Object> topics = extractTopics(nlpView, base);

        return new ParsedNlp(topics, namedEntities, posDistribution, sentenceRows, sentenceScores, sentiments, sarcasmRows);
    }

    private List<Map<String, Object>> extractSarcasmRows(JCas view, List<Map<String, Object>> sentenceRows) {
        List<Map<String, Object>> out = new ArrayList<>();
        Set<String> sarcasmTypes = new LinkedHashSet<>();
        for (Type type : allTypes(view)) {
            String name = type == null ? "" : String.valueOf(type.getName()).toLowerCase(Locale.ROOT);
            if (name.contains("sarcasm")) {
                sarcasmTypes.add(type.getName());
            }
        }
        for (String typeName : sarcasmTypes) {
            for (AnnotationFS fs : selectByTypeName(view, typeName)) {
                int begin = fs.getBegin();
                int end = fs.getEnd();
                if (begin < 0 || end <= begin) {
                    continue;
                }
                Double score = firstNumericFeature(fs,
                        "score", "value", "probability", "confidence", "sarcasm", "prediction");
                if (score == null) {
                    score = 0.0;
                }
                Map<String, Object> row = new HashMap<>();
                row.put("begin", begin);
                row.put("end", end);
                row.put("sentence", safeSpan(view.getDocumentText(), view.getDocumentText(), begin, end));
                row.put("score", score);
                out.add(row);
            }
        }

        if (out.isEmpty() && !sentenceRows.isEmpty()) {
            for (Map<String, Object> sentence : sentenceRows) {
                out.add(new HashMap<>(Map.of(
                        "begin", sentence.getOrDefault("begin", 0),
                        "end", sentence.getOrDefault("end", 0),
                        "sentence", String.valueOf(sentence.getOrDefault("sentence", "")),
                        "score", 0.0
                )));
            }
        }
        return out;
    }

    private List<Object> extractTopics(JCas nlpView, JCas base) {
        List<Object> topics = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String typeName : TOPIC_TYPE_CANDIDATES) {
            for (AnnotationFS fs : selectByTypeName(base, typeName)) {
                String label = firstStringFeature(fs, "topic", "label", "value", "name", "id");
                if (label == null || label.isBlank() || !seen.add(label)) {
                    continue;
                }
                Double score = firstNumericFeature(fs, "score", "confidence", "probability", "value");
                Map<String, Object> row = new HashMap<>();
                row.put("label", label);
                if (score != null) {
                    row.put("score", score);
                }
                topics.add(row);
            }
        }

        if (!topics.isEmpty()) {
            return topics;
        }

        for (AnnotationFS fs : selectByTypeName(base, T_TOPIC_CAT)) {
            String label = firstStringFeature(fs, "value", "topic", "label");
            if (label == null || label.isBlank() || !seen.add(label)) {
                continue;
            }
            Double score = firstNumericFeature(fs, "score", "confidence");
            Map<String, Object> row = new HashMap<>();
            row.put("label", label);
            if (score != null) {
                row.put("score", score);
            }
            topics.add(row);
        }

        if (topics.isEmpty() && nlpView != base) {
            for (String typeName : TOPIC_TYPE_CANDIDATES) {
                for (AnnotationFS fs : selectByTypeName(nlpView, typeName)) {
                    String label = firstStringFeature(fs, "topic", "label", "value", "name", "id");
                    if (label == null || label.isBlank() || !seen.add(label)) {
                        continue;
                    }
                    Double score = firstNumericFeature(fs, "score", "confidence", "probability", "value");
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", label);
                    if (score != null) {
                        row.put("score", score);
                    }
                    topics.add(row);
                }
            }
        }
        return topics;
    }

    private double meanSentimentForSpan(JCas view, int begin, int end) {
        double sum = 0.0;
        int count = 0;
        for (String typeName : SENTIMENT_TYPE_CANDIDATES) {
            for (AnnotationFS fs : selectCoveredByTypeName(view, begin, end, typeName)) {
                Double value = firstNumericFeature(fs,
                        "sentiment", "score", "value", "compound", "sentimentScore", "prediction");
                if (value == null) {
                    continue;
                }
                sum += value;
                count++;
            }
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private String readPosTag(AnnotationFS token) {
        try {
            Feature posFeature = token.getType().getFeatureByBaseName("pos");
            if (posFeature == null) {
                return null;
            }
            org.apache.uima.cas.FeatureStructure posFs = token.getFeatureValue(posFeature);
            if (posFs == null) {
                return null;
            }
            Type posType = posFs.getType();
            if (posType == null) {
                return null;
            }
            for (String field : List.of("PosValue", "posValue", "value", "Value")) {
                Feature f = posType.getFeatureByBaseName(field);
                if (f == null) {
                    continue;
                }
                String value = posFs.getStringValue(f);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return posType.getShortName();
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<AnnotationFS> selectByTypeName(JCas view, String typeName) {
        if (view == null || typeName == null || typeName.isBlank()) {
            return List.of();
        }
        Type type = view.getTypeSystem().getType(typeName);
        if (type == null) {
            return List.of();
        }
        List<AnnotationFS> out = new ArrayList<>();
        var iterator = view.getCas().getAnnotationIndex(type).iterator();
        while (iterator.hasNext()) {
            out.add((AnnotationFS) iterator.next());
        }
        return out;
    }

    private List<AnnotationFS> selectCoveredByTypeName(JCas view, int begin, int end, String typeName) {
        List<AnnotationFS> out = new ArrayList<>();
        for (AnnotationFS fs : selectByTypeName(view, typeName)) {
            if (fs.getBegin() >= begin && fs.getEnd() <= end) {
                out.add(fs);
            }
        }
        return out;
    }

    private List<Type> allTypes(JCas view) {
        List<Type> out = new ArrayList<>();
        var iterator = view.getTypeSystem().getTypeIterator();
        while (iterator.hasNext()) {
            out.add(iterator.next());
        }
        return out;
    }

    private String firstStringFeature(AnnotationFS fs, String... names) {
        if (fs == null || fs.getType() == null) {
            return null;
        }
        for (String name : names) {
            Feature feature = fs.getType().getFeatureByBaseName(name);
            if (feature == null) {
                continue;
            }
            try {
                String value = fs.getStringValue(feature);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Double firstNumericFeature(AnnotationFS fs, String... names) {
        if (fs == null || fs.getType() == null) {
            return null;
        }
        for (String name : names) {
            Feature feature = fs.getType().getFeatureByBaseName(name);
            if (feature == null) {
                continue;
            }
            try {
                String asText = fs.getFeatureValueAsString(feature);
                if (asText == null || asText.isBlank()) {
                    continue;
                }
                return Double.parseDouble(asText);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String safeSpan(String preferredText, String fallbackText, int begin, int end) {
        String text = preferredText;
        if (text == null || text.isBlank()) {
            text = fallbackText == null ? "" : fallbackText;
        }
        if (begin < 0 || end <= begin || end > text.length()) {
            return "";
        }
        return text.substring(begin, end);
    }

    private record ParsedNlp(
            List<Object> topics,
            List<Object> namedEntities,
            Map<String, Integer> posDistribution,
            List<Map<String, Object>> sentenceRows,
            List<Double> sentenceScores,
            List<Double> sentiments,
            List<Map<String, Object>> sarcasmRows
    ) {
    }
}
