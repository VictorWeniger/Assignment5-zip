package org.example.service;

/**
 * Developer guide: Orchestrates DUUI execution, text normalization, and timestamp enrichment.
 */

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
 * Coordinates NLP annotation runs for speeches and persists processing metadata.
 */
public class NlpProcessingService {
    private final DatabaseHandler<Speech> speechDatabase;
    private final NlpEngine primaryEngine;
    private final VideoSentenceTimestampService videoSentenceTimestampService;

    /**
     * Creates a service with one primary NLP engine.
     */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase) {
        this(speechDatabase, (NlpEngine) null);
    }

    /**
     * Creates a service with a custom primary engine.
     */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase, NlpEngine primaryEngine) {
        this(speechDatabase, primaryEngine, null);
    }

    /**
     * Creates a service with a custom primary engine and a speech-video store for timestamp enrichment.
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
     * Creates a service with one primary engine and optional video timestamp enrichment.
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
     * Runs NLP processing on speeches up to {@code limit}.
     *
     * @param limit maximum number of speeches to process
     * @param force reprocess speeches even if they were already processed
     * @return summary containing processed and skipped counts
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

            // Batch mode is DUUI-only now: if the engine fails, the run fails instead of falling back.
            annotateWithPrimaryEngine(speech, force);
            speech.setNlpProcessed(true);
            speech.setNlpProcessedAt(Instant.now());
            speechDatabase.replaceById("speeches", speech.getId(), speech);
            processed++;
        }

        return new NlpRunSummary(processed, skipped);
    }

    /**
     * Runs NLP processing for one speech by id.
     *
     * @param speechId speech identifier
     * @param force reprocess speech even if it was already processed
     * @return summary containing processed and skipped counts
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
     * Returns aggregate counters for NLP processing progress.
     */
    public Document stats() {
        long total = speechDatabase.count("speeches", new Document());
        long processed = speechDatabase.count("speeches", new Document("nlpProcessed", true));
        return new Document("totalSpeeches", total)
                .append("nlpProcessedSpeeches", processed)
                .append("nlpPendingSpeeches", Math.max(0, total - processed));
    }

    private void annotateWithPrimaryEngine(Speech speech, boolean force) {
        // Ensure all downstream NLP/timestamp steps operate on repaired text.
        normalizeSpeechTextIfNeeded(speech);
        if (primaryEngine == null) {
            throw new IllegalStateException("No DUUI NLP engine is configured");
        }
        primaryEngine.annotate(speech);

        ensureEngineMetadata(speech, primaryEngine.name());
        if (videoSentenceTimestampService != null) {
            // Adds sentence-level t0/t1 when video/transcript data is available.
            videoSentenceTimestampService.enrichSpeech(speech, force);
        }
        // Keep a compact, stable serialization payload for export/debugging.
        UimaCasSerializer.enrichSpeechWithUimaCas(speech);
    }

    private void maybeEnrichTimestampsOnly(Speech speech, boolean force) {
        if (videoSentenceTimestampService == null || speech == null) {
            return;
        }
        // Even when NLP is skipped, we may still improve timing metadata.
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
        // Persist the engine name so the UI can distinguish DUUI output from imported XMI data.
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
     * Value object returned by NLP run operations.
     *
     * @param processed number of speeches processed
     * @param skipped number of speeches skipped
     */
    public record NlpRunSummary(int processed, int skipped) {
    }
}
