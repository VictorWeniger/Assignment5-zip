package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.Speech;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NlpProcessingServiceTest {
    @Test
    void processesSpeechAndWritesNlpFields() {
        InMemorySpeechDb db = new InMemorySpeechDb();
        Speech speech = new Speech();
        speech.setId("s1");
        speech.setText("Berlin ist gut. Die Krise ist schlecht.");
        db.insert("speeches", speech);

        NlpProcessingService service = new NlpProcessingService(db, new InlineEngine("duui-composer"));
        NlpProcessingService.NlpRunSummary summary = service.run(10, true);

        assertEquals(1, summary.processed());
        Speech stored = db.findById("speeches", "s1", Speech.class).orElseThrow();
        assertTrue(stored.isNlpProcessed());
        assertTrue(stored.getNlp().containsKey("sentenceSentiments"));
        assertTrue(stored.getNlp().containsKey("namedEntities"));
        assertTrue(stored.getNlp().containsKey("coreferences"));
        assertTrue(stored.getNlp().containsKey("uimaTypeSystem"));
        assertTrue(stored.getNlp().containsKey("uimaSummary"));
        assertEquals("duui-composer", String.valueOf(stored.getNlp().get("processingEngine")));
    }

    @Test
    void failsWhenPrimaryEngineFails() {
        InMemorySpeechDb db = new InMemorySpeechDb();
        Speech speech = new Speech();
        speech.setId("s2");
        speech.setText("Test text.");
        db.insert("speeches", speech);

        NlpProcessingService service = new NlpProcessingService(db, new FailingEngine());
        assertThrows(IllegalStateException.class, () -> service.run(10, true));

        Speech stored = db.findById("speeches", "s2", Speech.class).orElseThrow();
        assertTrue(!stored.isNlpProcessed());
    }

    private static class FailingEngine implements org.example.service.nlp.NlpEngine {
        @Override
        public String name() {
            return "failing";
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void annotate(Speech speech) {
            throw new IllegalStateException("boom");
        }
    }

    private static class InlineEngine implements org.example.service.nlp.NlpEngine {
        private final String name;

        private InlineEngine(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void annotate(Speech speech) {
            java.util.Map<String, Object> nlp = new java.util.HashMap<>();
            nlp.put("sentenceSentiments", List.of(Map.of("score", 0.0)));
            nlp.put("namedEntities", List.of(Map.of("text", "Berlin", "type", "LOC", "begin", 0, "end", 6)));
            nlp.put("coreferences", List.of(Map.of("label", "berlin", "mentions", List.of(Map.of("begin", 0, "end", 6)))));
            speech.setNlp(nlp);
            speech.setSentenceSentiments(List.of(0.0));
            speech.setNamedEntities(List.of(Map.of("text", "Berlin", "type", "LOC", "begin", 0, "end", 6)));
        }
    }

    private static class InMemorySpeechDb implements DatabaseHandler<Speech> {
        private final List<Speech> speeches = new ArrayList<>();

        @Override
        public void insert(String collection, Speech entity) {
            speeches.add(entity);
        }

        @Override
        public void replaceById(String collection, String id, Speech entity) {
            deleteById(collection, id);
            speeches.add(entity);
        }

        @Override
        public Optional<Speech> findById(String collection, String id, Class<Speech> type) {
            return speeches.stream().filter(s -> id.equals(s.getId())).findFirst();
        }

        @Override
        public List<Speech> find(String collection, Document filter, Class<Speech> type) {
            return new ArrayList<>(speeches);
        }

        @Override
        public long count(String collection, Document filter) {
            if (filter.containsKey("nlpProcessed")) {
                boolean v = filter.getBoolean("nlpProcessed", false);
                return speeches.stream().filter(s -> s.isNlpProcessed() == v).count();
            }
            return speeches.size();
        }

        @Override
        public List<Document> aggregate(String collection, List<Document> pipeline) {
            return List.of();
        }

        @Override
        public void deleteById(String collection, String id) {
            speeches.removeIf(s -> id.equals(s.getId()));
        }
    }
}
