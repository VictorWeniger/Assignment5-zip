package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.example.service.nlp.NlpEngine;
import org.example.service.nlp.TextNormalizationUtil;
import org.example.service.nlp.UimaCasSerializer;
import org.example.service.nlp.VideoSentenceTimestampService;
import org.bson.Document;

import java.time.Instant;
import java.util.List;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpProcessingService service
 */
public class NlpProcessingService {
    private final DatabaseHandler<Speech> speechDatabase;
    private final NlpEngine primaryEngine;
    private final VideoSentenceTimestampService videoSentenceTimestampService;

/**
 * Constructor
 */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase) {
        this(speechDatabase, (NlpEngine) null);
    }

/**
 * Constructor
 */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase, NlpEngine primaryEngine) {
        this(speechDatabase, primaryEngine, null);
    }

/**
 * Constructor
 */
    public NlpProcessingService(
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<SpeechVideo> speechVideoDatabase,
            NlpEngine primaryEngine
    ) {
        this(
                speechDatabase,
                primaryEngine,
                new VideoSentenceTimestampService(speechVideoDatabase)
        );
    }

/**
 * Constructor
 */
    public NlpProcessingService(
            DatabaseHandler<Speech> speechDatabase,
            NlpEngine primaryEngine,
            VideoSentenceTimestampService videoSentenceTimestampService
    ) {
        this.speechDatabase = speechDatabase;
        this.primaryEngine = primaryEngine;
        this.videoSentenceTimestampService = videoSentenceTimestampService;
    }

/**
 * Method
 */
    public NlpRunSummary run(int limit, boolean force) {
        Document filter = force
                ? new Document()
                : new Document("nlpProcessed", new Document("$ne", true));
        List<Speech> speeches = speechDatabase.findLimited("speeches", filter, Speech.class, limit);
        int processed = 0;
        int skipped = 0;

        for (Speech speech : speeches) {
            if (speech == null || speech.getId() == null || speech.getId().isBlank()) {
                skipped++;
                continue;
            }

            annotateWithPrimaryEngine(speech, force);
            speech.setNlpProcessed(true);
            speech.setNlpProcessedAt(Instant.now());
            speechDatabase.replaceById("speeches", speech.getId(), speech);
            processed++;
        }

        return new NlpRunSummary(processed, skipped);
    }

/**
 * Method
 */
    public NlpRunSummary runSingleSpeech(String speechId, boolean force) {
        Speech speech = speechDatabase.findById("speeches", speechId, Speech.class).orElse(null);
        if (speech == null) {
            return new NlpRunSummary(0, 1);
        }
        if (speech.isNlpProcessed() && !force) {
            maybeEnrichTimestampsOnly(speech, false);
            return new NlpRunSummary(0, 1);
        }

        annotateWithPrimaryEngine(speech, force);
        speech.setNlpProcessed(true);
        speech.setNlpProcessedAt(Instant.now());
        speechDatabase.replaceById("speeches", speech.getId(), speech);
        return new NlpRunSummary(1, 0);
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

    private void annotateWithPrimaryEngine(Speech speech, boolean force) {
        normalizeSpeechTextIfNeeded(speech);
        if (primaryEngine == null) {
            throw new IllegalStateException("No DUUI NLP engine is configured");
        }
        primaryEngine.annotate(speech);

        ensureEngineMetadata(speech, primaryEngine.name());
        if (videoSentenceTimestampService != null) {
            videoSentenceTimestampService.enrichSpeech(speech, force);
        }
        UimaCasSerializer.enrichSpeechWithUimaCas(speech);
    }

    private void maybeEnrichTimestampsOnly(Speech speech, boolean force) {
        if (videoSentenceTimestampService == null || speech == null) {
            return;
        }
        normalizeSpeechTextIfNeeded(speech);
        videoSentenceTimestampService.enrichSpeech(speech, force);
        UimaCasSerializer.enrichSpeechWithUimaCas(speech);
        if (speech.getId() != null && !speech.getId().isBlank()) {
            speechDatabase.replaceById("speeches", speech.getId(), speech);
        }
    }

    private void ensureEngineMetadata(Speech speech, String engineName) {
        if (speech.getNlp() == null) {
            speech.setNlp(new java.util.HashMap<>());
        }
        speech.getNlp().put("processingEngine", engineName);
    }

    private void normalizeSpeechTextIfNeeded(Speech speech) {
        if (speech == null) {
            return;
        }
        String raw = speech.getText();
        String normalized = TextNormalizationUtil.sanitizeSpeechText(raw);
        if (raw == null || !raw.equals(normalized)) {
            speech.setText(normalized);
        }
    }

/**
 * NlpRunSummary service
 */
    public record NlpRunSummary(int processed, int skipped) {
    }
}
