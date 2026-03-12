package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.Speech;
import org.example.service.nlp.UimaCasSerializer;
import org.bson.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpAnnotationImportService service
 */
public class NlpAnnotationImportService {
    private static final Pattern XMI_DOCUMENT_ID_PATTERN = Pattern.compile("^ID(\\d{2})(\\d{3})\\d+$", Pattern.CASE_INSENSITIVE);
    private static final List<Path> DEFAULT_IMPORT_CANDIDATES = List.of(
            Path.of(".local-data", "nlp-dokumente-abschlussprojekt-main", "xmi"),
            Path.of(".local-data", "nlp-dokumente-abschlussprojekt-main"),
            Path.of("nlp-dokumente-abschlussprojekt-main", "xmi"),
            Path.of("nlp-dokumente-abschlussprojekt-main"),
            Path.of("xmi"),
            Path.of(".")
    );

    private final DatabaseHandler<Speech> speechDatabase;

/**
 * Constructor
 */
    public NlpAnnotationImportService(DatabaseHandler<Speech> speechDatabase) {
        this.speechDatabase = speechDatabase;
    }

/**
 * Method
 */
    public ImportResult importFromFile(String filePath, boolean createMissing) {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }
        return importFromPath(path, createMissing);
    }

/**
 * Method
 */
    public ImportResult importFromDefaultLocation(boolean createMissing) {
        Path path = resolveDefaultImportPath();
        return importFromPath(path, createMissing);
    }

/**
 * Method
 */
    public String describeDefaultImportPath() {
        return resolveDefaultImportPath().toString();
    }

    private ImportResult importFromPath(Path path, boolean createMissing) {
        int processed = 0;
        int updated = 0;
        int created = 0;
        int skipped = 0;

        try {
            List<Path> files = resolveImportTargets(path);
            for (Path target : files) {
                processed++;
                ImportOutcome outcome = importSingle(target, createMissing);
                if (outcome == ImportOutcome.UPDATED) {
                    updated++;
                } else if (outcome == ImportOutcome.CREATED) {
                    created++;
                } else {
                    skipped++;
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read file: " + path, e);
        }

        return new ImportResult(processed, updated, created, skipped);
    }

    private Path resolveDefaultImportPath() {
        for (Path candidate : DEFAULT_IMPORT_CANDIDATES) {
            if (!Files.exists(candidate)) {
                continue;
            }
            try {
                if (Files.isDirectory(candidate)) {
                    try (java.util.stream.Stream<Path> stream = Files.walk(candidate)) {
                        boolean containsXmi = stream
                                .filter(Files::isRegularFile)
                                .anyMatch(this::isSupportedImportFile);
                        if (containsXmi) {
                            return candidate;
                        }
                    }
                } else if (isSupportedImportFile(candidate)) {
                    return candidate;
                }
            } catch (IOException ignored) {
            }
        }
        throw new IllegalArgumentException(
                "No local XMI import source found. Expected a directory such as .local-data/nlp-dokumente-abschlussprojekt-main/xmi."
        );
    }

/**
 * Method
 */
    public Document stats() {
        long total = speechDatabase.count("speeches", new Document());
        long processed = speechDatabase.count("speeches", new Document("nlpProcessed", true));
        return new Document("totalSpeeches", total)
                .append("nlpProcessedSpeeches", processed)
                .append("nlpPendingSpeeches", Math.max(0, total - processed));
    }

    private List<Path> resolveImportTargets(Path path) throws IOException {
        if (!Files.isDirectory(path)) {
            return List.of(path);
        }

        try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isSupportedImportFile)
                    .sorted(Comparator.naturalOrder())
                    .toList();
        }
    }

    private boolean isSupportedImportFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".xmi")
                || name.endsWith(".xmi.gz");
    }

    private ImportOutcome importSingle(Path path, boolean createMissing) {
        return importFromXmi(path, createMissing);
    }

    private ImportOutcome importFromXmi(Path path, boolean createMissing) {
        org.w3c.dom.Document xml = parseXml(path);
        String speechId = readDocumentId(xml, path);
        if (speechId.isBlank()) {
            return ImportOutcome.SKIPPED;
        }

        String text = readSofaString(xml);
        String speakerId = readSpeakerId(xml);
        Speech speech = resolveExistingSpeech(speechId, text, speakerId);
        boolean exists = speech != null;
        if (speech == null) {
            speech = getOrCreateSpeech(speechId, text, createMissing);
        }
        if (speech == null) {
            return ImportOutcome.SKIPPED;
        }

        speech.setText(text);
        Map<String, Object> nlp = ensureNlpMap(speech);
        nlp.put("sourceFormat", "uima-xmi");
        nlp.put("sourceFile", path.toString());
        nlp.put("documentId", speechId);
        nlp.put("annotationComments", extractAnnotationComments(xml));

        List<Map<String, Object>> sentenceSentiments = extractSentenceSentiments(xml);
        List<Map<String, Object>> sentenceSarcasm = extractSentenceSarcasm(xml);
        List<Map<String, Object>> entities = extractNamedEntities(xml, text);
        List<Map<String, Object>> topics = extractTopics(xml, text);
        Map<String, Integer> posDistribution = extractPosDistribution(xml);

        speech.setSentenceSentiments(sentenceSentiments.stream()
                .map(m -> asDouble(m.get("score")))
                .filter(java.util.Objects::nonNull)
                .toList());
        speech.setSentiments(extractOverallSentiments(xml));
        speech.setNamedEntities(new ArrayList<>(entities));
        speech.setTopics(new ArrayList<>(topics));
        speech.setPosDistribution(posDistribution);

        nlp.put("sentenceSentiments", sentenceSentiments);
        nlp.put("sentenceSarcasm", sentenceSarcasm);
        nlp.put("namedEntities", entities);
        nlp.put("topics", topics);
        nlp.put("posDistribution", posDistribution);
        nlp.put("rawTypeSystemPath", resolveAdjacentTypeSystem(path));

        finalizeAndStore(speech);
        return exists ? ImportOutcome.UPDATED : ImportOutcome.CREATED;
    }

    private Speech resolveExistingSpeech(String speechId, String text, String speakerId) {
        Optional<Speech> existing = speechDatabase.findById("speeches", speechId, Speech.class);
        if (existing.isPresent()) {
            return existing.get();
        }

        String protocolId = inferProtocolId(speechId);
        if (protocolId == null) {
            return null;
        }

        List<Speech> candidates = speechDatabase.find("speeches", new Document("protocolId", protocolId), Speech.class);
        if (candidates.isEmpty()) {
            return null;
        }

        List<Speech> narrowed = candidates.stream()
                .filter(speech -> speakerId == null || speakerId.isBlank() || matchesSpeakerId(speech, speakerId))
                .toList();
        if (narrowed.isEmpty()) {
            narrowed = candidates;
        }

        String normalizedTarget = normalizeText(text);
        if (normalizedTarget.isBlank()) {
            return null;
        }

        for (Speech candidate : narrowed) {
            if (normalizedTarget.equals(normalizeText(candidate.getText()))) {
                return candidate;
            }
        }

        for (Speech candidate : narrowed) {
            String normalizedCandidate = normalizeText(candidate.getText());
            if (normalizedCandidate.isBlank()) {
                continue;
            }
            if ((normalizedCandidate.contains(normalizedTarget) || normalizedTarget.contains(normalizedCandidate))
                    && Math.min(normalizedCandidate.length(), normalizedTarget.length()) >= 200) {
                return candidate;
            }
        }

        return null;
    }

    private Speech getOrCreateSpeech(String speechId, String text, boolean createMissing) {
        Optional<Speech> existing = speechDatabase.findById("speeches", speechId, Speech.class);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (!createMissing) {
            return null;
        }
        Speech speech = new Speech();
        speech.setId(speechId);
        speech.setText(text);
        return speech;
    }

    private void finalizeAndStore(Speech speech) {
        speech.setNlpProcessed(true);
        speech.setNlpProcessedAt(Instant.now());
        UimaCasSerializer.enrichSpeechWithUimaCas(speech);
        speechDatabase.replaceById("speeches", speech.getId(), speech);
    }

    private Map<String, Object> ensureNlpMap(Speech speech) {
        if (speech.getNlp() == null) {
            speech.setNlp(new LinkedHashMap<>());
        }
        return speech.getNlp();
    }

    private org.w3c.dom.Document parseXml(Path path) {
        try (InputStream raw = Files.newInputStream(path);
             InputStream input = path.getFileName().toString().toLowerCase().endsWith(".gz")
                     ? new GZIPInputStream(raw)
                     : raw) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(input);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse XMI file: " + path, e);
        }
    }

    private String readDocumentId(org.w3c.dom.Document xml, Path path) {
        Element meta = firstElement(xml, "DocumentMetaData");
        if (meta != null) {
            String documentId = meta.getAttribute("documentId");
            if (!documentId.isBlank()) {
                return documentId;
            }
        }
        String name = path.getFileName().toString();
        if (name.endsWith(".xmi.gz")) {
            return name.substring(0, name.length() - 7);
        }
        if (name.endsWith(".xmi")) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private String readSofaString(org.w3c.dom.Document xml) {
        Element sofa = firstElement(xml, "Sofa");
        return sofa == null ? "" : sofa.getAttribute("sofaString");
    }

    private String readSpeakerId(org.w3c.dom.Document xml) {
        NodeList nodes = xml.getElementsByTagNameNS("*", "AnnotationComment");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            if ("speaker.id".equals(el.getAttribute("key"))) {
                String value = el.getAttribute("value");
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private List<Map<String, Object>> extractNamedEntities(org.w3c.dom.Document xml, String text) {
        List<Map<String, Object>> entities = new ArrayList<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "NamedEntity");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            int begin = intAttr(el, "begin", -1);
            int end = intAttr(el, "end", -1);
            if (begin < 0 || end < begin) {
                continue;
            }
            Map<String, Object> entity = new LinkedHashMap<>();
            entity.put("begin", begin);
            entity.put("end", end);
            entity.put("type", el.getAttribute("value"));
            entity.put("text", safeSubstring(text, begin, end));
            entities.add(entity);
        }
        return entities;
    }

    private Map<String, Integer> extractPosDistribution(org.w3c.dom.Document xml) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "POS");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            String tag = el.getAttribute("coarseValue");
            if (tag.isBlank()) {
                tag = el.getAttribute("PosValue");
            }
            if (!tag.isBlank()) {
                counts.merge(tag, 1, Integer::sum);
            }
        }
        return counts;
    }

    private List<Map<String, Object>> extractSentenceSentiments(org.w3c.dom.Document xml) {
        List<Map<String, Object>> sentiments = new ArrayList<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "GerVaderSentiment");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            int begin = intAttr(el, "begin", -1);
            int end = intAttr(el, "end", -1);
            if (begin < 0 || end < begin) {
                continue;
            }
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("begin", begin);
            value.put("end", end);
            value.put("score", doubleAttr(el, "sentiment", 0.0));
            value.put("pos", doubleAttr(el, "pos", 0.0));
            value.put("neu", doubleAttr(el, "neu", 0.0));
            value.put("neg", doubleAttr(el, "neg", 0.0));
            sentiments.add(value);
        }
        return sentiments;
    }

    private List<Double> extractOverallSentiments(org.w3c.dom.Document xml) {
        List<Map<String, Object>> sentenceSentiments = extractSentenceSentiments(xml);
        if (sentenceSentiments.isEmpty()) {
            return List.of();
        }
        Map<String, Object> first = sentenceSentiments.get(0);
        return List.of(asDouble(first.get("score")) == null ? 0.0 : asDouble(first.get("score")));
    }

    private List<Map<String, Object>> extractSentenceSarcasm(org.w3c.dom.Document xml) {
        List<Map<String, Object>> sarcasm = new ArrayList<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "Sarcasm");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            int begin = intAttr(el, "begin", -1);
            int end = intAttr(el, "end", -1);
            if (begin < 0 || end < begin) {
                continue;
            }
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("begin", begin);
            value.put("end", end);
            value.put("score", doubleAttr(el, "Sarcasm", 0.0));
            value.put("nonSarcasm", doubleAttr(el, "NonSarcasm", 0.0));
            sarcasm.add(value);
        }
        return sarcasm;
    }

    private List<Map<String, Object>> extractTopics(org.w3c.dom.Document xml, String text) {
        List<Map<String, Object>> topics = new ArrayList<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "CategoryCoveredTagged");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            int begin = intAttr(el, "begin", -1);
            int end = intAttr(el, "end", -1);
            Map<String, Object> topic = new LinkedHashMap<>();
            topic.put("label", el.getAttribute("value"));
            topic.put("score", doubleAttr(el, "score", 0.0));
            if (begin >= 0 && end >= begin) {
                topic.put("begin", begin);
                topic.put("end", end);
                topic.put("text", safeSubstring(text, begin, end));
            }
            topics.add(topic);
        }
        return topics;
    }

    private List<Map<String, Object>> extractAnnotationComments(org.w3c.dom.Document xml) {
        List<Map<String, Object>> comments = new ArrayList<>();
        NodeList nodes = xml.getElementsByTagNameNS("*", "AnnotationComment");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            Map<String, Object> comment = new LinkedHashMap<>();
            copyAttrIfPresent(el, comment, "reference");
            copyAttrIfPresent(el, comment, "key");
            copyAttrIfPresent(el, comment, "value");
            if (!comment.isEmpty()) {
                comments.add(comment);
            }
        }
        return comments;
    }

    private void copyAttrIfPresent(Element element, Map<String, Object> target, String name) {
        String value = element.getAttribute(name);
        if (!value.isBlank()) {
            target.put(name, value);
        }
    }

    private String resolveAdjacentTypeSystem(Path xmiPath) {
        Path sibling = xmiPath.getParent() == null
                ? Path.of("TypeSystem.xml.gz")
                : xmiPath.getParent().resolve("TypeSystem.xml.gz");
        if (Files.exists(sibling)) {
            return sibling.toString();
        }
        return "";
    }

    private Element firstElement(org.w3c.dom.Document xml, String localName) {
        NodeList nodes = xml.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return (Element) nodes.item(0);
    }

    private String inferProtocolId(String speechId) {
        Matcher matcher = XMI_DOCUMENT_ID_PATTERN.matcher(speechId);
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1)) + "-" + Integer.parseInt(matcher.group(2));
    }

    private boolean matchesSpeakerId(Speech speech, String speakerId) {
        return speech.getSpeaker() != null
                && speech.getSpeaker().getId() != null
                && speakerId.equals(speech.getSpeaker().getId());
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private int intAttr(Element element, String attr, int defaultValue) {
        String value = element.getAttribute(attr);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double doubleAttr(Element element, String attr, double defaultValue) {
        String value = element.getAttribute(attr);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String safeSubstring(String text, int begin, int end) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int safeBegin = Math.max(0, Math.min(begin, text.length()));
        int safeEnd = Math.max(safeBegin, Math.min(end, text.length()));
        return text.substring(safeBegin, safeEnd);
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    private enum ImportOutcome {
        CREATED,
        UPDATED,
        SKIPPED
    }

/**
 * ImportResult service
 */
    public record ImportResult(int processed, int updated, int created, int skipped) {
    }
}
