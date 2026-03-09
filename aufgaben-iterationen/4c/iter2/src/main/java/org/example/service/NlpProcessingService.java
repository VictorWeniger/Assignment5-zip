package org.example.service;

/**
 * Developer guide: Orchestrates NLP engine execution, fallback logic, text normalization, and timestamp enrichment.
 */

import org.example.db.DatabaseHandler;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.example.service.nlp.LocalHeuristicNlpEngine;
import org.example.service.nlp.NlpEngine;
import org.example.service.nlp.TextNormalizationUtil;
import org.example.service.nlp.UimaCasSerializer;
import org.example.service.nlp.VideoSentenceTimestampService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;

/**
 * Coordinates NLP annotation runs for speeches and persists processing metadata.
 */
public class NlpProcessingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NlpProcessingService.class);

    private final DatabaseHandler<Speech> speechDatabase;
    private final NlpEngine primaryEngine;
    private final NlpEngine fallbackEngine;
    private final VideoSentenceTimestampService videoSentenceTimestampService;

    /**
     * Creates a service that uses the local heuristic engine as primary and fallback engine.
     */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase) {
        this(speechDatabase, new LocalHeuristicNlpEngine(), new LocalHeuristicNlpEngine(), null);
    }

    /**
     * Creates a service with a custom primary engine and local fallback engine.
     */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase, NlpEngine primaryEngine) {
        this(speechDatabase, primaryEngine, new LocalHeuristicNlpEngine(), null);
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
                null,
                new VideoSentenceTimestampService(speechVideoDatabase)
        );
    }

    /**
     * Creates a service with explicit primary and fallback engines.
     */
    public NlpProcessingService(DatabaseHandler<Speech> speechDatabase, NlpEngine primaryEngine, NlpEngine fallbackEngine) {
        this(speechDatabase, primaryEngine, fallbackEngine, null);
    }

    /**
     * Creates a service with explicit primary/fallback engines and optional video timestamp enrichment.
     */
    public NlpProcessingService(
            DatabaseHandler<Speech> speechDatabase,
            NlpEngine primaryEngine,
            NlpEngine fallbackEngine,
            VideoSentenceTimestampService videoSentenceTimestampService
    ) {
        this.speechDatabase = speechDatabase;
        this.primaryEngine = primaryEngine;
        this.fallbackEngine = fallbackEngine;
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
        List<Speech> speeches = speechDatabase.find("speeches", new Document(), Speech.class);
        int processed = 0;
        int skipped = 0;

        for (Speech speech : speeches) {
            if (processed >= limit) {
                break;
            }
            if (speech == null || speech.getId() == null || speech.getId().isBlank()) {
                skipped++;
                continue;
            }
            if (speech.isNlpProcessed() && !force) {
                maybeEnrichTimestampsOnly(speech, false);
                skipped++;
                continue;
            }

            annotateWithFallback(speech, force);
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

        annotateWithFallback(speech, force);
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

    private void annotateWithFallback(Speech speech, boolean force) {
        // Ensure all downstream NLP/timestamp steps operate on repaired text.
        normalizeSpeechTextIfNeeded(speech);
        String engineName;
        try {
            primaryEngine.annotate(speech);
            engineName = primaryEngine.name();
        } catch (RuntimeException ex) {
            if (fallbackEngine == null || primaryEngine.name().equals(fallbackEngine.name())) {
                throw ex;
            }
            if (speech.getNlp() == null) {
                speech.setNlp(new java.util.HashMap<>());
            }
            speech.getNlp().put("duuiError", ex.getMessage());
            LOGGER.warn("NLP engine '{}' failed for speech {}. Falling back to '{}'. Cause: {}",
                    primaryEngine.name(), speech.getId(), fallbackEngine.name(), ex.getMessage());
            fallbackEngine.annotate(speech);
            engineName = fallbackEngine.name();
        }

        ensureEngineMetadata(speech, engineName);
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
