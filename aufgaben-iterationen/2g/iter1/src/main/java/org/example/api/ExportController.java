package org.example.api;

/**
 * Developer guide: Exposes TeX/PDF export endpoints and template-driven export orchestration.
 */

import org.example.db.DatabaseHandler;
import org.example.model.ExportTemplate;
import org.example.model.Speech;
import org.example.service.tex.TeXPdfCompiler;
import org.example.service.tex.TeXSpeechExporter;
import io.javalin.Javalin;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers export endpoints for LaTeX and PDF generation.
 */
public final class ExportController {
    private ExportController() {
    }

    /**
     * Registers export routes and wires template lookup, filtering, and PDF compilation.
     */
    public static void register(
            Javalin app,
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<ExportTemplate> templateDatabase
    ) {
        TeXSpeechExporter exporter = new TeXSpeechExporter();
        TeXPdfCompiler pdfCompiler = new TeXPdfCompiler();

        app.get("/api/export/tex", ctx -> {
            try {
                int limit = parseInt(ctx.queryParam("limit"), 200, "limit", 1000);
                Document filter = SpeechQueryFilterBuilder.build(
                        ctx.queryParam("protocolId"),
                        ctx.queryParam("protocolIds"),
                        null,
                        ctx.queryParam("speakerId"),
                        ctx.queryParam("faction"),
                        ctx.queryParam("topic"),
                        null,
                        ctx.queryParam("matchMode")
                );
                List<Speech> speeches = speechDatabase.find("speeches", filter, Speech.class);
                Instant from = parseInstantNullable(ctx.queryParam("from"), "from");
                Instant to = parseInstantNullable(ctx.queryParam("to"), "to");
                speeches = filterByRange(speeches, from, to);
                if (speeches.size() > limit) {
                    speeches = new ArrayList<>(speeches.subList(0, limit));
                }
                String title = ctx.queryParam("title");
                if (title == null || title.isBlank()) {
                    title = "Parlamentsreden Export";
                }
                boolean includeTikz = parseBoolean(ctx.queryParam("includeTikz"), false, "includeTikz");

                String tex = exporter.exportSpeeches(title, speeches, loadTemplates(templateDatabase), ctx.queryParam("groupBy"), includeTikz);
                ctx.contentType("text/plain; charset=utf-8");
                ctx.result(tex);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/export/pdf", ctx -> {
            try {
                int limit = parseInt(ctx.queryParam("limit"), 200, "limit", 1000);
                Document filter = SpeechQueryFilterBuilder.build(
                        ctx.queryParam("protocolId"),
                        ctx.queryParam("protocolIds"),
                        null,
                        ctx.queryParam("speakerId"),
                        ctx.queryParam("faction"),
                        ctx.queryParam("topic"),
                        null,
                        ctx.queryParam("matchMode")
                );
                List<Speech> speeches = speechDatabase.find("speeches", filter, Speech.class);
                Instant from = parseInstantNullable(ctx.queryParam("from"), "from");
                Instant to = parseInstantNullable(ctx.queryParam("to"), "to");
                speeches = filterByRange(speeches, from, to);
                if (speeches.size() > limit) {
                    speeches = new ArrayList<>(speeches.subList(0, limit));
                }
                String title = ctx.queryParam("title");
                if (title == null || title.isBlank()) {
                    title = "Parlamentsreden Export";
                }
                boolean includeTikz = parseBoolean(ctx.queryParam("includeTikz"), false, "includeTikz");

                String tex = exporter.exportSpeeches(title, speeches, loadTemplates(templateDatabase), ctx.queryParam("groupBy"), includeTikz);
                TeXPdfCompiler.CompileResult result = pdfCompiler.compile(tex);
                if (!result.success()) {
                    ctx.status(501).json(new Document("error", result.message()).append("details", result.compilerOutput()));
                    return;
                }

                ctx.contentType("application/pdf");
                ctx.header("Content-Disposition", "inline; filename=\"export.pdf\"");
                ctx.result(result.pdfBytes());
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });
    }

    /**
     * Loads export templates from persistent storage into a map keyed by template id.
     */
    private static Map<String, String> loadTemplates(DatabaseHandler<ExportTemplate> templateDatabase) {
        List<ExportTemplate> rows = templateDatabase.find("export_templates", new Document(), ExportTemplate.class);
        Map<String, String> map = new HashMap<>();
        for (ExportTemplate row : rows) {
            if (row.getId() != null && row.getContent() != null) {
                map.put(row.getId(), row.getContent());
            }
        }
        return map;
    }

    /**
     * Applies an optional start/end instant filter to speech start timestamps.
     */
    private static List<Speech> filterByRange(List<Speech> speeches, Instant from, Instant to) {
        if (from == null && to == null) {
            return speeches;
        }
        return speeches.stream().filter(speech -> {
            Instant startedAt = speech.getStartedAt();
            if (startedAt == null) {
                return false;
            }
            if (from != null && startedAt.isBefore(from)) {
                return false;
            }
            if (to != null && startedAt.isAfter(to)) {
                return false;
            }
            return true;
        }).toList();
    }

    /**
     * Parses an optional ISO-8601 instant parameter.
     */
    private static Instant parseInstantNullable(String input, String name) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(input);
        } catch (Exception ignored) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 instant");
        }
    }

    /**
     * Parses and validates a non-negative integer with an upper bound.
     */
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

    /**
     * Parses and validates a boolean query parameter.
     */
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
