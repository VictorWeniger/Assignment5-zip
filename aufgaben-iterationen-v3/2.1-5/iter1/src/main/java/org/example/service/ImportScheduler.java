package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author
 * Victor Weniger
 */

/**
 * ImportScheduler service
 */
public class ImportScheduler implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImportScheduler.class);

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

/**
 * Method
 */
    public void schedule(ProtocolImportService importService, Duration interval) {
        long minutes = Math.max(1, interval.toMinutes());

        executorService.scheduleAtFixedRate(() -> {
            ProtocolImportService.ImportSummary summary = importService.importMissingProtocols();
            LOGGER.info(
                    "Scheduled import finished: protocols={}, sessions={}, speeches={}, deputies={}, videos={}, images={}, skippedSpeeches={}, skippedDeputies={}, skippedVideos={}",
                    summary.importedProtocols(),
                    summary.upsertedSessions(),
                    summary.upsertedSpeeches(),
                    summary.upsertedDeputies(),
                    summary.upsertedVideos(),
                    summary.enrichedDeputyImages(),
                    summary.skippedInvalidSpeeches(),
                    summary.skippedInvalidDeputies(),
                    summary.skippedInvalidVideos()
            );
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    @Override

/**
 * Method
 */
    public void close() {
        executorService.shutdownNow();
    }
}
