package org.example.api;

/**
 * Developer guide: Exposes protocol and video import endpoints with input validation.
 */

import org.example.service.ProtocolImportService;
import org.example.service.AgendaVideoImportService;
import io.javalin.Javalin;
import org.bson.Document;

/**
 * Registers protocol import endpoints (preview, scoped run, single run).
 */
public final class ImportController {
    private static final int MAX_IMPORT_LIMIT = 500;

    private ImportController() {
    }

    /**
     * Registers import routes.
     */
    public static void register(Javalin app, ProtocolImportService importService, AgendaVideoImportService agendaVideoImportService) {
        app.post("/api/import/run/{protocolId}", ctx -> {
            String protocolId = ctx.pathParam("protocolId");
            if (protocolId == null || protocolId.isBlank()) {
                ctx.status(400).json(new Document("error", "protocolId is required"));
                return;
            }
            try {
                boolean force = parseBoolean(ctx.queryParam("force"), false, "force");
                ctx.json(importService.importSingleProtocol(protocolId, force));
            } catch (IllegalStateException ex) {
                ctx.status(409).json(new Document("error", ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/import/preview", ctx -> {
            try {
                Integer legislativePeriod = parseNullableInt(ctx.queryParam("period"), "period");
                int maxProtocols = parseInt(ctx.queryParam("limit"), 0, "limit");
                boolean force = parseBoolean(ctx.queryParam("force"), false, "force");
                ctx.json(importService.previewImportCandidates(
                        new ProtocolImportService.ImportOptions(legislativePeriod, maxProtocols, force)
                ));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.post("/api/import/run", ctx -> {
            try {
                Integer legislativePeriod = parseNullableInt(ctx.queryParam("period"), "period");
                int maxProtocols = parseInt(ctx.queryParam("limit"), 0, "limit");
                boolean force = parseBoolean(ctx.queryParam("force"), false, "force");

                ProtocolImportService.ImportSummary summary = importService.importProtocols(
                        new ProtocolImportService.ImportOptions(legislativePeriod, maxProtocols, force)
                );
                ctx.json(summary);
            } catch (IllegalStateException ex) {
                ctx.status(409).json(new Document("error", ex.getMessage()));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.post("/api/import/videos/agenda", ctx -> {
            try {
                String protocolId = ctx.queryParam("protocolId");
                int agendaItem = parseInt(ctx.queryParam("agendaItem"), 0, "agendaItem");
                String sessionUrl = ctx.queryParam("sessionUrl");
                boolean download = parseBoolean(ctx.queryParam("download"), false, "download");
                int maxSpeeches = parseInt(ctx.queryParam("maxSpeeches"), 0, "maxSpeeches");
                ctx.json(agendaVideoImportService.importAgendaVideos(protocolId, agendaItem, sessionUrl, download, maxSpeeches));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.post("/api/import/videos/auto", ctx -> {
            try {
                String sessionUrl = ctx.queryParam("sessionUrl");
                boolean download = parseBoolean(ctx.queryParam("download"), false, "download");
                int maxSpeeches = parseInt(ctx.queryParam("maxSpeeches"), 5, "maxSpeeches");
                ctx.json(agendaVideoImportService.importBestAgendaVideos(sessionUrl, download, maxSpeeches));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.post("/api/import/videos/speech/{speechId}", ctx -> {
            try {
                ctx.json(agendaVideoImportService.importVideosForSpeech(ctx.pathParam("speechId")));
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });
    }

    private static Integer parseNullableInt(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            if ("period".equals(name) && (parsed < 1 || parsed > 99)) {
                throw new IllegalArgumentException("period must be between 1 and 99");
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static int parseInt(String value, int fallback, String name) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            if ("limit".equals(name) && parsed > MAX_IMPORT_LIMIT) {
                throw new IllegalArgumentException("limit must be <= " + MAX_IMPORT_LIMIT);
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static boolean parseBoolean(String value, boolean fallback, String name) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be true or false");
    }
}
