package org.example.service;

import org.example.model.Comment;
import org.example.model.Deputy;
import org.example.model.ParliamentaryGroup;
import org.example.model.ProtocolSession;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.XMLConstants;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.xml.sax.InputSource;

/**
 * @author
 * Victor Weniger
 */

/**
 * XmlProtocolParser service
 */
public class XmlProtocolParser {

/**
 * Method
 */
    public ParsedProtocol parse(String protocolId, int legislativePeriod, String xml) {
        org.w3c.dom.Document doc = parseXml(xml);
        ProtocolSession session = new ProtocolSession();
        session.setId("session-" + protocolId);
        session.setProtocolId(protocolId);
        session.setLegislativePeriod(legislativePeriod);
        session.setSessionNumber(parseInt(findFirstText(doc, "sitzungsnr", "sitzungsnummer"), 0));
        session.setSessionDate(parseDate(findFirstText(doc, "sitzungsdatum", "datum")));

        extractAgenda(doc, session);

        Map<String, Deputy> deputyById = new LinkedHashMap<>();
        List<SpeechVideo> videos = new ArrayList<>();
        List<Speech> speeches = extractSpeeches(doc, protocolId, session.getId(), session.getSessionDate(), deputyById, videos);
        for (Speech speech : speeches) {
            session.getSpeechIds().add(speech.getId());
        }

        return new ParsedProtocol(session, speeches, new ArrayList<>(deputyById.values()), videos);
    }

    private org.w3c.dom.Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse protocol XML: " + e.getMessage(), e);
        }
    }

    private void extractAgenda(org.w3c.dom.Document doc, ProtocolSession session) {
        for (org.w3c.dom.Element top : descendants(doc, "tagesordnungspunkt")) {
            String label = firstNonBlank(
                    directChildText(top, "titel"),
                    directChildText(top, "toptext"),
                    text(top)
            );
            if (!label.isBlank()) {
                session.getAgenda().add(label);
            }
        }
    }

    private List<Speech> extractSpeeches(
            org.w3c.dom.Document doc,
            String protocolId,
            String sessionId,
            LocalDate sessionDate,
            Map<String, Deputy> deputyById,
            List<SpeechVideo> videos
    ) {
        List<Speech> result = new ArrayList<>();
        int[] index = {0};
        walkSpeechNodes(doc.getDocumentElement(), 0, protocolId, sessionId, sessionDate, deputyById, videos, result, index);
        return result;
    }

    private void walkSpeechNodes(
            Node node,
            int currentAgendaItem,
            String protocolId,
            String sessionId,
            LocalDate sessionDate,
            Map<String, Deputy> deputyById,
            List<SpeechVideo> videos,
            List<Speech> result,
            int[] index
    ) {
        if (!(node instanceof org.w3c.dom.Element element)) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                walkSpeechNodes(children.item(i), currentAgendaItem, protocolId, sessionId, sessionDate, deputyById, videos, result, index);
            }
            return;
        }

        int effectiveAgendaItem = currentAgendaItem;
        if ("tagesordnungspunkt".equalsIgnoreCase(element.getTagName())) {
            int parsedAgendaItem = parseAgendaItemFromNode(element);
            if (parsedAgendaItem > 0) {
                effectiveAgendaItem = parsedAgendaItem;
            }
        }

        if ("rede".equalsIgnoreCase(element.getTagName())) {
            Speech speech = new Speech();
            speech.setId(resolveSpeechId(element, protocolId, ++index[0]));
            speech.setProtocolId(protocolId);
            speech.setSessionId(sessionId);

            int directAgendaItem = parseAgendaItem(element);
            speech.setAgendaItem(directAgendaItem > 0 ? directAgendaItem : effectiveAgendaItem);
            speech.setStartedAt(parseSpeechStart(element, sessionDate));
            speech.setEndedAt(parseSpeechEnd(element, sessionDate));

            Deputy speaker = parseSpeaker(element, protocolId, index[0]);
            speech.setSpeaker(speaker);
            deputyById.putIfAbsent(speaker.getId(), speaker);

            speech.setText(extractSpeechText(element));
            parseComments(element, speech);
            SpeechVideo video = parseSpeechVideo(element, speech);
            if (video != null) {
                videos.add(video);
            }
            result.add(speech);
            return;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            walkSpeechNodes(children.item(i), effectiveAgendaItem, protocolId, sessionId, sessionDate, deputyById, videos, result, index);
        }
    }

    private Deputy parseSpeaker(org.w3c.dom.Element rede, String protocolId, int index) {
        Deputy speaker = new Deputy();
        org.w3c.dom.Element redner = firstDescendant(rede, "redner");

        String speakerId = firstNonBlank(
                attr(redner, "id"),
                nestedText(rede, "redner", "id")
        );
        if (speakerId.isBlank()) {
            speakerId = "deputy-" + protocolId + "-" + index;
        }
        speaker.setId(speakerId);

        speaker.setTitle(firstNonBlank(
                nestedText(rede, "redner", "titel"),
                attr(redner, "titel")
        ));
        speaker.setFirstName(firstNonBlank(
                nestedText(rede, "redner", "vorname"),
                attr(redner, "vorname")
        ));
        speaker.setLastName(firstNonBlank(
                nestedText(rede, "redner", "nachname"),
                attr(redner, "nachname")
        ));

        if (speaker.getFirstName() == null || speaker.getFirstName().isBlank()) {
            String name = firstNonBlank(
                    nestedText(rede, "redner", "name"),
                    attr(redner, "name")
            );
            String[] parts = name.split("\\s+", 2);
            if (parts.length > 0) {
                speaker.setFirstName(parts[0]);
            }
            if (parts.length > 1) {
                speaker.setLastName(parts[1]);
            }
        }

        String factionText = firstNonBlank(
                nestedText(rede, "redner", "fraktion"),
                textOfFirst(rede, "fraktion"),
                attr(redner, "fraktion"),
                attr(redner, "fraktion-kurz")
        );
        if (!factionText.isBlank()) {
            ParliamentaryGroup group = new ParliamentaryGroup();
            group.setId(toFactionId(factionText));
            group.setShortName(factionText);
            group.setDisplayName(factionText);
            speaker.setParliamentaryGroup(group);
        }

        return speaker;
    }

    private SpeechVideo parseSpeechVideo(org.w3c.dom.Element rede, Speech speech) {
        String rawUrl = firstNonBlank(
                attr(rede, "video-url"),
                attr(rede, "videourl"),
                attr(rede, "mediathek-url"),
                attr(rede, "video"),
                nestedText(rede, "video", "url"),
                textOfFirst(rede, "video-url", "mediathek-url", "url")
        );
        String videoId = firstNonBlank(
                attr(rede, "videoid"),
                attr(rede, "video-id"),
                textOfFirst(rede, "video-id", "videoid"),
                extractBundestagVideoId(rawUrl)
        );
        if ((rawUrl == null || rawUrl.isBlank()) && (videoId == null || videoId.isBlank())) {
            return null;
        }

        String videoPageUrl = null;
        String streamUrl = null;
        String sourceUrl = rawUrl;
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            String lower = sourceUrl.toLowerCase();
            if (lower.contains(".mp4") || lower.contains(".m3u8")) {
                streamUrl = sourceUrl;
                if (videoId != null && !videoId.isBlank()) {
                    videoPageUrl = toBundestagVideoPage(videoId);
                }
            } else if (sourceUrl.contains("bundestag.de/mediathek")) {
                videoPageUrl = sourceUrl;
            }
        }
        if ((videoPageUrl == null || videoPageUrl.isBlank()) && videoId != null && !videoId.isBlank()) {
            videoPageUrl = toBundestagVideoPage(videoId);
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            sourceUrl = firstNonBlank(streamUrl, videoPageUrl);
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }

        SpeechVideo video = new SpeechVideo();
        video.setId("video-" + speech.getId());
        video.setSpeechId(speech.getId());
        video.setSourceUrl(sourceUrl);
        video.setVideoPageUrl(videoPageUrl);
        video.setStreamUrl(streamUrl);
        if (videoPageUrl != null && !videoPageUrl.isBlank()) {
            video.setEmbedUrl(toBundestagEmbedUrl(videoPageUrl));
        }
        video.setDurationSeconds(0);
        return video;
    }

    private String toBundestagVideoPage(String videoId) {
        return "https://www.bundestag.de/mediathek/video?videoid=" + videoId;
    }

    private String toBundestagEmbedUrl(String videoPageUrl) {
        String videoId = extractBundestagVideoId(videoPageUrl);
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        return "https://www.bundestag.de/mediathekoverlay?videoid=" + videoId + "&mod=mediathek";
    }

    private String extractBundestagVideoId(String value) {
        if (value == null || value.isBlank() || !value.contains("videoid=")) {
            return null;
        }
        int start = value.indexOf("videoid=");
        if (start < 0) {
            return null;
        }
        String rest = value.substring(start + "videoid=".length());
        int end = rest.indexOf('&');
        return end >= 0 ? rest.substring(0, end) : rest;
    }

    private void parseComments(org.w3c.dom.Element rede, Speech speech) {
        int idx = 0;
        int scanOffset = 0;
        for (org.w3c.dom.Element c : descendants(rede, "kommentar", "interjection", "zwischenruf")) {
            String value = text(c);
            if (value.isBlank()) {
                continue;
            }
            Comment comment = new Comment();
            comment.setId(speech.getId() + "-comment-" + (++idx));
            comment.setSpeechId(speech.getId());
            comment.setText(value);

            if (speech.getText() == null) {
                comment.setSpeechOffset(0);
            } else {
                int found = speech.getText().indexOf(value, scanOffset);
                if (found < 0) {
                    found = speech.getText().indexOf(value);
                }
                comment.setSpeechOffset(Math.max(0, found));
                if (found >= 0) {
                    scanOffset = found + value.length();
                }
            }

            String author = firstNonBlank(
                    attr(c, "redner"),
                    nestedText(c, "redner"),
                    attr(c, "urheber"),
                    nestedText(c, "name")
            );
            comment.setAuthorName(author);

            String faction = firstNonBlank(
                    attr(c, "fraktion"),
                    nestedText(c, "fraktion")
            );
            comment.setAuthorFaction(faction);
            speech.getComments().add(comment);
        }
    }

    private int parseAgendaItem(org.w3c.dom.Element rede) {
        String top = firstNonBlank(
                attr(rede, "top-id"),
                attr(rede, "tagesordnungspunkt"),
                attr(rede, "top"),
                textOfFirst(rede, "top-id")
        );
        int parsed = parseInt(top.replaceAll("[^0-9]", ""), 0);
        if (parsed > 0) {
            return parsed;
        }

        Node parent = rede.getParentNode();
        while (parent != null) {
            if (parent instanceof org.w3c.dom.Element element && "tagesordnungspunkt".equalsIgnoreCase(element.getTagName())) {
                return parseAgendaItemFromNode(element);
            }
            parent = parent.getParentNode();
        }
        return 0;
    }

    private int parseAgendaItemFromNode(org.w3c.dom.Element node) {
        String top = firstNonBlank(
                attr(node, "top-id"),
                attr(node, "tagesordnungspunkt"),
                attr(node, "top"),
                attr(node, "id"),
                directChildText(node, "top-id"),
                directChildText(node, "top"),
                directChildText(node, "topnr"),
                directChildText(node, "topnummer"),
                textOfFirst(node, "top-id", "top", "topnr", "topnummer")
        );
        return parseInt(top.replaceAll("[^0-9]", ""), 0);
    }

    private String extractSpeechText(org.w3c.dom.Element rede) {
        StringBuilder builder = new StringBuilder();
        for (org.w3c.dom.Element p : descendants(rede, "p")) {
            String value = text(p);
            if (!value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append('\n');
                }
                builder.append(value);
            }
        }
        if (builder.isEmpty()) {
            builder.append(text(rede));
        }
        return builder.toString();
    }

    private Instant parseSpeechStart(org.w3c.dom.Element rede, LocalDate sessionDate) {
        String raw = firstNonBlank(
                attr(rede, "start"),
                attr(rede, "beginn"),
                attr(rede, "beginn-uhrzeit"),
                textOfFirst(rede, "beginn", "start")
        );
        return parseInstant(raw, sessionDate);
    }

    private Instant parseSpeechEnd(org.w3c.dom.Element rede, LocalDate sessionDate) {
        String raw = firstNonBlank(
                attr(rede, "ende"),
                attr(rede, "ende-uhrzeit"),
                textOfFirst(rede, "ende", "end")
        );
        return parseInstant(raw, sessionDate);
    }

    private String resolveSpeechId(org.w3c.dom.Element rede, String protocolId, int index) {
        String xmlId = attr(rede, "id");
        if (!xmlId.isBlank()) {
            return xmlId;
        }
        String externalId = textOfFirst(rede, "rede-id");
        if (!externalId.isBlank()) {
            return externalId;
        }
        return "speech-" + protocolId + "-" + index + "-" + UUID.randomUUID();
    }

    private String findFirstText(org.w3c.dom.Document doc, String... names) {
        return textOfFirst(doc, names);
    }

    private String textOfFirst(Node node, String... names) {
        for (String name : names) {
            org.w3c.dom.Element element = firstDescendant(node, name);
            if (element != null) {
                String value = text(element);
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private String nestedText(Node node, String... path) {
        Node current = node;
        for (String name : path) {
            current = firstDescendant(current, name);
            if (current == null) {
                return "";
            }
        }
        return text(current);
    }

    private String directChildText(Node node, String name) {
        for (org.w3c.dom.Element child : children(node, name)) {
            String value = text(child);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private List<org.w3c.dom.Element> descendants(Node node, String... names) {
        List<org.w3c.dom.Element> out = new ArrayList<>();
        for (String name : names) {
            if (node instanceof org.w3c.dom.Document document) {
                NodeList list = document.getElementsByTagName(name);
                for (int i = 0; i < list.getLength(); i++) {
                    if (list.item(i) instanceof org.w3c.dom.Element element) {
                        out.add(element);
                    }
                }
            } else if (node instanceof org.w3c.dom.Element element) {
                NodeList list = element.getElementsByTagName(name);
                for (int i = 0; i < list.getLength(); i++) {
                    if (list.item(i) instanceof org.w3c.dom.Element match) {
                        out.add(match);
                    }
                }
            }
        }
        return out;
    }

    private List<org.w3c.dom.Element> children(Node node, String name) {
        List<org.w3c.dom.Element> out = new ArrayList<>();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof org.w3c.dom.Element element && name.equals(element.getTagName())) {
                out.add(element);
            }
        }
        return out;
    }

    private org.w3c.dom.Element firstDescendant(Node node, String name) {
        if (node instanceof org.w3c.dom.Document document) {
            NodeList list = document.getElementsByTagName(name);
            return list.getLength() > 0 && list.item(0) instanceof org.w3c.dom.Element element ? element : null;
        }
        if (node instanceof org.w3c.dom.Element element) {
            NodeList list = element.getElementsByTagName(name);
            return list.getLength() > 0 && list.item(0) instanceof org.w3c.dom.Element match ? match : null;
        }
        return null;
    }

    private String attr(org.w3c.dom.Element element, String name) {
        if (element == null || !element.hasAttribute(name)) {
            return "";
        }
        return clean(element.getAttribute(name));
    }

    private String text(Node node) {
        return clean(node == null ? "" : node.getTextContent());
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\u00A0', ' ').trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = clean(value);
            if (!cleaned.isBlank()) {
                return cleaned;
            }
        }
        return "";
    }

    private String toFactionId(String factionName) {
        return factionName.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private LocalDate parseDate(String raw) {
        String normalized = clean(raw).replace('.', '-');
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(normalized);
        }
        if (normalized.matches("\\d{2}-\\d{2}-\\d{4}")) {
            String[] parts = normalized.split("-");
            return LocalDate.of(parseInt(parts[2], 1970), parseInt(parts[1], 1), parseInt(parts[0], 1));
        }
        return null;
    }

    private Instant parseInstant(String raw, LocalDate sessionDate) {
        String normalized = clean(raw);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(normalized);
        } catch (Exception ignored) {
        }
        if (sessionDate != null && normalized.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            String padded = normalized.length() == 5 ? normalized + ":00" : normalized;
            LocalTime localTime = LocalTime.parse(padded);
            return LocalDateTime.of(sessionDate, localTime)
                    .atZone(ZoneId.of("Europe/Berlin"))
                    .toInstant();
        }
        return null;
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

/**
 * ParsedProtocol service
 */
    public record ParsedProtocol(
            ProtocolSession session,
            List<Speech> speeches,
            List<Deputy> deputies,
            List<SpeechVideo> videos
    ) {
    }
}
