package org.example.api;

import org.example.service.NlpProcessingService;
import org.example.service.NlpAnnotationImportService;
import io.javalin.Javalin;
import org.bson.Document;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpController controller
 */
public final class NlpController {
    private NlpController() {
    }

/**
 * Method
 */
    public static void register(Javalin app, NlpProcessingService nlpService, NlpAnnotationImportService annotationImportService) {
        app.post("/api/nlp/run/{speechId}", ctx -> {
            String speechId = ctx.pathParam("speechId");
            if (speechId == null || speechId.isBlank()) {
                ctx.status(400).json(new Document("error", "speechId is required"));
                return;
            }
            boolean force;
            try {
                force = parseBoolean(ctx.queryParam("force"), false, "force");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }
            ctx.json(nlpService.runSingleSpeech(speechId, force));
        });

        app.post("/api/nlp/run", ctx -> {
            int limit;
            boolean force;
            try {
                limit = parseInt(ctx.queryParam("limit"), 300, "limit", 5000);
                force = parseBoolean(ctx.queryParam("force"), false, "force");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }

            ctx.json(nlpService.run(limit, force));
        });

        app.get("/api/nlp/stats", ctx -> ctx.json(nlpService.stats()));

        app.post("/api/nlp/import", ctx -> {
            String path = ctx.queryParam("path");
            boolean createMissing;
            try {
                createMissing = parseBoolean(ctx.queryParam("createMissing"), false, "createMissing");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }

            try {
                if (path == null || path.isBlank()) {
                    ctx.json(new Document("sourcePath", annotationImportService.describeDefaultImportPath())
                            .append("result", annotationImportService.importFromDefaultLocation(createMissing)));
                } else {
                    ctx.json(new Document("sourcePath", path)
                            .append("result", annotationImportService.importFromFile(path, createMissing)));
                }
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });
    }

    private static int parseInt(String input, int fallback, String name, int maxValue) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(input);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            if (parsed > maxValue) {
                throw new IllegalArgumentException(name + " must be <= " + maxValue);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static boolean parseBoolean(String input, boolean fallback, String name) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(input)) {
            return true;
        }
        if ("false".equalsIgnoreCase(input)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be true or false");
    }
}
