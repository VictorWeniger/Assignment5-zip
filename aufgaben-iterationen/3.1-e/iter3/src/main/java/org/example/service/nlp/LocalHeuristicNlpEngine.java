package org.example.service.nlp;

/**
 * Developer guide: In-process fallback NLP engine using lightweight heuristic extraction rules.
 */

import org.example.model.Speech;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight in-process NLP engine using rule-based heuristics for demo/test environments.
 */
public class LocalHeuristicNlpEngine implements NlpEngine {
    private static final Set<String> POSITIVE = Set.of("gut", "stark", "erfolg", "chance", "verbessern", "positiv", "sicher");
    private static final Set<String> NEGATIVE = Set.of("problem", "krise", "schlecht", "risiko", "negativ", "fehler", "konflikt");
    // Keep the local demo-NER from tagging common sentence starters/UI words as entities.
    private static final Set<String> ENTITY_STOPWORDS = Set.of(
            "Sehr", "Werte", "Denn", "Daher", "Diese", "Dieses", "Wir", "Mit", "Das", "Bitte",
            "Vielen", "Fuer", "Für", "Ihre", "Genau", "So", "In", "Es", "Sie", "An", "Zudem"
    );
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?", Pattern.MULTILINE);
    private static final Pattern TOKEN = Pattern.compile("[\\p{L}][\\p{L}\\p{Mn}\\p{Pd}']*");
    private static final List<String> SARCASM_MARKERS = List.of(
            "ja klar",
            "natuerlich",
            "natürlich",
            "als ob",
            "wie ueberraschend",
            "wie überraschend",
            "sicher doch",
            "haha"
    );

    /**
     * Engine identifier used in processing metadata.
     */
    @Override
    public String name() {
        return "local";
    }

    /**
     * The local heuristic engine is always available in-process.
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Annotates one speech with heuristic topics, entities, sentiments, POS, and comment attribution metadata.
     */
    @Override
    public void annotate(Speech speech) {
        String text = speech.getText() == null ? "" : speech.getText();

        List<Map<String, Object>> sentenceSentiments = new ArrayList<>();
        List<Map<String, Object>> sentenceSarcasm = new ArrayList<>();
        List<Double> sentiments = new ArrayList<>();
        Map<String, Integer> posDist = new HashMap<>();
        List<Object> entities = new ArrayList<>();
        List<Object> topics = new ArrayList<>();
        List<Object> coreferences = new ArrayList<>();
        List<Object> commentAttributions = new ArrayList<>();

        Map<String, Integer> topicCounts = new HashMap<>();
        Map<String, List<Map<String, Object>>> corefMentions = new LinkedHashMap<>();

        Matcher sentenceMatcher = SENTENCE_PATTERN.matcher(text);
        while (sentenceMatcher.find()) {
            String sentence = sentenceMatcher.group().trim();
            if (sentence.isBlank()) {
                continue;
            }
            int begin = sentenceMatcher.start();
            int end = sentenceMatcher.end();

            double sentiment = sentimentScore(sentence);
            sentiments.add(sentiment);

            Map<String, Object> sentRow = new HashMap<>();
            sentRow.put("sentence", sentence);
            sentRow.put("begin", begin);
            sentRow.put("end", end);
            sentRow.put("score", sentiment);
            sentenceSentiments.add(sentRow);

            Map<String, Object> sarcasmRow = new HashMap<>();
            sarcasmRow.put("sentence", sentence);
            sarcasmRow.put("begin", begin);
            sarcasmRow.put("end", end);
            sarcasmRow.put("score", sarcasmScore(sentence, sentiment));
            sentenceSarcasm.add(sarcasmRow);

            Matcher tokenMatcher = TOKEN.matcher(sentence);
            while (tokenMatcher.find()) {
                String token = tokenMatcher.group();
                int tokenBegin = begin + tokenMatcher.start();
                int tokenEnd = begin + tokenMatcher.end();

                if (token.length() >= 1
                        && Character.isUpperCase(token.charAt(0))
                        && token.length() > 2
                        && !ENTITY_STOPWORDS.contains(token)
                        // Short ALLCAPS tokens are often faction acronyms (SPD/FDP/CDU), not entities.
                        && !(isAllCaps(token) && token.length() <= 5)
                        // Avoid tagging the very first token of each sentence headline-style.
                        && tokenBegin > begin) {
                    Map<String, Object> ne = new HashMap<>();
                    ne.put("type", "PER_OR_MISC");
                    ne.put("text", token);
                    ne.put("begin", tokenBegin);
                    ne.put("end", tokenEnd);
                    entities.add(ne);

                    String key = token.toLowerCase();
                    corefMentions.computeIfAbsent(key, ignored -> new ArrayList<>()).add(Map.of(
                            "text", token,
                            "begin", tokenBegin,
                            "end", tokenEnd
                    ));
                }

                String lowered = token.toLowerCase();
                if (lowered.endsWith("en")) {
                    posDist.put("VERB", posDist.getOrDefault("VERB", 0) + 1);
                } else if (lowered.endsWith("ung") || lowered.endsWith("keit") || lowered.endsWith("heit")) {
                    posDist.put("NOUN", posDist.getOrDefault("NOUN", 0) + 1);
                } else {
                    posDist.put("OTHER", posDist.getOrDefault("OTHER", 0) + 1);
                }

                String topic = classifyTopic(lowered);
                if (topic != null) {
                    topicCounts.put(topic, topicCounts.getOrDefault(topic, 0) + 1);
                }
            }
        }

        for (Map.Entry<String, Integer> t : topicCounts.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("label", t.getKey());
            row.put("score", t.getValue());
            topics.add(row);
        }

        for (Map.Entry<String, List<Map<String, Object>>> e : corefMentions.entrySet()) {
            if (e.getValue().size() < 2) {
                continue;
            }
            List<Map<String, Object>> mentions = new ArrayList<>(e.getValue());
            mentions.sort(Comparator.comparingInt(m -> (int) m.get("begin")));

            Map<String, Object> group = new HashMap<>();
            group.put("label", e.getKey());
            group.put("mentions", mentions);
            coreferences.add(group);
        }

        Map<String, Object> nlp = new HashMap<>();
        nlp.put("topics", topics);
        nlp.put("namedEntities", entities);
        nlp.put("coreferences", coreferences);
        nlp.put("posDistribution", posDist);
        nlp.put("sentenceSentiments", sentenceSentiments);
        nlp.put("sentenceSarcasm", sentenceSarcasm);
        nlp.put("commentAttributions", commentAttributions);

        speech.setNlp(nlp);
        speech.setTopics(topics);
        speech.setNamedEntities(entities);
        speech.setPosDistribution(posDist);
        speech.setSentenceSentiments(extractSentenceSentimentValues(sentenceSentiments));
        speech.setSentiments(sentiments);
        inferCommentAttributions(speech, commentAttributions);
    }

    private List<Double> extractSentenceSentimentValues(List<Map<String, Object>> rows) {
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object score = row.get("score");
            if (score instanceof Number n) {
                values.add(n.doubleValue());
            }
        }
        return values;
    }

    private double sentimentScore(String sentence) {
        int pos = 0;
        int neg = 0;
        Matcher m = TOKEN.matcher(sentence.toLowerCase());
        while (m.find()) {
            String token = m.group();
            if (POSITIVE.contains(token)) {
                pos++;
            }
            if (NEGATIVE.contains(token)) {
                neg++;
            }
        }
        if (pos == 0 && neg == 0) {
            return 0.0;
        }
        return (double) (pos - neg) / (pos + neg);
    }

    private String classifyTopic(String token) {
        if (token.contains("klima") || token.contains("energie") || token.contains("umwelt")) {
            return "Climate";
        }
        if (token.contains("wirtschaft") || token.contains("arbeit") || token.contains("industrie")) {
            return "Economy";
        }
        if (token.contains("gesund") || token.contains("pflege") || token.contains("medizin")) {
            return "Health";
        }
        if (token.contains("bildung") || token.contains("schule") || token.contains("universit")) {
            return "Education";
        }
        return null;
    }

    private double sarcasmScore(String sentence, double sentiment) {
        String lowered = sentence.toLowerCase();
        double score = 0.0;
        for (String marker : SARCASM_MARKERS) {
            if (lowered.contains(marker)) {
                score += 0.55;
            }
        }
        if (lowered.contains("!")) {
            score += 0.1;
        }
        if (lowered.contains("?")) {
            score += 0.1;
        }
        if (Math.abs(sentiment) > 0.75) {
            score += 0.15;
        }
        return Math.min(1.0, score);
    }

    private void inferCommentAttributions(Speech speech, List<Object> commentAttributions) {
        if (speech.getComments() == null || speech.getComments().isEmpty()) {
            return;
        }

        Set<String> detectedFactions = new HashSet<>();
        Map<String, String> factionPatterns = Map.of(
                "SPD", "(?i)\\bSPD\\b|Sozialdemokrat",
                "CDU/CSU", "(?i)\\bCDU\\b|\\bCSU\\b|Union",
                "B90/GRUENE", "(?i)\\bGRUENE\\b|B90",
                "FDP", "(?i)\\bFDP\\b|Freie Demokraten",
                "AfD", "(?i)\\bAfD\\b|Alternative fuer Deutschland",
                "DIE LINKE", "(?i)\\bLINKE\\b"
        );

        for (var comment : speech.getComments()) {
            String text = comment.getText() == null ? "" : comment.getText();
            String author = comment.getAuthorName() == null ? "" : comment.getAuthorName().trim();
            String faction = comment.getAuthorFaction() == null ? "" : comment.getAuthorFaction().trim();
            String resolvedAuthor = author;
            String resolvedFaction = faction;
            String source = "existing";
            double confidence = 1.0;

            if (resolvedFaction.isBlank()) {
                for (Map.Entry<String, String> pattern : factionPatterns.entrySet()) {
                    if (Pattern.compile(pattern.getValue()).matcher(text).find()) {
                        resolvedFaction = pattern.getKey();
                        source = "nlp-faction-pattern";
                        confidence = 0.72;
                        break;
                    }
                }
            }

            if (resolvedAuthor.isBlank()) {
                String speakerName = speakerDisplayName(speech);
                if (!speakerName.isBlank()) {
                    resolvedAuthor = speakerName;
                    source = "heuristic-speaker-fallback";
                    confidence = Math.min(confidence, 0.45);
                } else {
                    resolvedAuthor = "Unbekannt";
                    source = "unknown";
                    confidence = 0.2;
                }
            }

            if (resolvedFaction.isBlank() && speech.getSpeaker() != null
                    && speech.getSpeaker().getParliamentaryGroup() != null
                    && speech.getSpeaker().getParliamentaryGroup().getShortName() != null
                    && !speech.getSpeaker().getParliamentaryGroup().getShortName().isBlank()) {
                resolvedFaction = speech.getSpeaker().getParliamentaryGroup().getShortName();
                source = "speaker-faction-fallback";
                confidence = Math.min(confidence, 0.35);
            }

            if (!resolvedFaction.isBlank()) {
                detectedFactions.add(resolvedFaction);
                comment.setAuthorFaction(resolvedFaction);
            }
            comment.setAuthorName(resolvedAuthor);

            Map<String, Object> attribution = new LinkedHashMap<>();
            attribution.put("commentId", comment.getId() == null ? "" : comment.getId());
            attribution.put("speechOffset", comment.getSpeechOffset());
            attribution.put("authorName", resolvedAuthor);
            attribution.put("authorFaction", resolvedFaction);
            attribution.put("source", source);
            attribution.put("confidence", confidence);
            commentAttributions.add(attribution);
        }

        if (speech.getNlp() != null) {
            speech.getNlp().put("detectedCommentFactions", new ArrayList<>(detectedFactions));
        }
    }

    private String speakerDisplayName(Speech speech) {
        if (speech.getSpeaker() == null) {
            return "";
        }
        String first = speech.getSpeaker().getFirstName() == null ? "" : speech.getSpeaker().getFirstName().trim();
        String last = speech.getSpeaker().getLastName() == null ? "" : speech.getSpeaker().getLastName().trim();
        return (first + " " + last).trim();
    }

    private boolean isAllCaps(String token) {
        // "All caps" check constrained to letters; punctuation/hyphens are ignored.
        boolean hasLetter = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (!Character.isUpperCase(c)) {
                    return false;
                }
            }
        }
        return hasLetter;
    }
}
