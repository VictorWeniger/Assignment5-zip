package org.example.api;

import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

final class SpeechQueryFilterBuilder {
    private SpeechQueryFilterBuilder() {
    }

    static Document build(
            String protocolId,
            String protocolIdsCsv,
            String sessionId,
            String speakerId,
            String faction,
            String topic,
            Integer agendaItem,
            String matchModeInput
    ) {
        MatchMode matchMode = parseMatchMode(matchModeInput);
        List<Document> criteria = new ArrayList<>();

        List<String> protocolIds = parseCsvList(protocolIdsCsv);
        if (protocolId != null && !protocolId.isBlank()) {
            protocolIds.add(protocolId.trim());
        }
        if (!protocolIds.isEmpty()) {
            if (protocolIds.size() == 1) {
                criteria.add(new Document("protocolId", protocolIds.get(0)));
            } else {
                criteria.add(new Document("protocolId", new Document("$in", protocolIds)));
            }
        }

        appendEqualsCriterion(criteria, "sessionId", sessionId);
        appendEqualsCriterion(criteria, "speaker.id", speakerId);
        appendEqualsCriterion(criteria, "speaker.parliamentaryGroup.shortName", faction);
        if (agendaItem != null) {
            criteria.add(new Document("agendaItem", agendaItem));
        }
        if (topic != null && !topic.isBlank()) {
            criteria.add(topicCriterion(topic));
        }

        return composeCriteria(criteria, matchMode);
    }

    static List<String> parseCsvList(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isBlank() && !out.contains(trimmed)) {
                out.add(trimmed);
            }
        }
        return out;
    }

    private static void appendEqualsCriterion(List<Document> criteria, String field, String value) {
        if (value != null && !value.isBlank()) {
            criteria.add(new Document(field, value.trim()));
        }
    }

    private static Document topicCriterion(String topic) {
        return new Document("$or", List.of(
                new Document("nlp.topics.label", topic),
                new Document("nlp.topics.topic", topic),
                new Document("topics.label", topic),
                new Document("topics.topic", topic)
        ));
    }

    private static MatchMode parseMatchMode(String input) {
        if (input == null || input.isBlank() || "and".equalsIgnoreCase(input)) {
            return MatchMode.AND;
        }
        if ("or".equalsIgnoreCase(input)) {
            return MatchMode.OR;
        }
        throw new IllegalArgumentException("matchMode must be 'and' or 'or'");
    }

    private static Document composeCriteria(List<Document> criteria, MatchMode matchMode) {
        if (criteria.isEmpty()) {
            return new Document();
        }
        if (criteria.size() == 1) {
            return criteria.getFirst();
        }
        return switch (matchMode) {
            case AND -> new Document("$and", criteria);
            case OR -> new Document("$or", criteria);
        };
    }

    private enum MatchMode {
        AND,
        OR
    }
}
