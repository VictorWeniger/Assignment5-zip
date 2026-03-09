package org.example.api;

/**
 * Developer guide: Manages export templates (seed, read, update).
 */

import org.example.db.DatabaseHandler;
import org.example.model.ExportTemplate;
import io.javalin.Javalin;
import org.bson.Document;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Registers CRUD-like endpoints for export template management.
 */
public final class TemplateController {
    private TemplateController() {
    }

    /**
     * Registers template listing, lookup, upsert, and default seeding routes.
     */
    public static void register(Javalin app, DatabaseHandler<ExportTemplate> templateDatabase) {
        app.get("/api/templates", ctx -> {
            int limit = parseInt(ctx.queryParam("limit"), 200);
            List<ExportTemplate> templates = templateDatabase.find("export_templates", new Document(), ExportTemplate.class);
            ctx.json(templates.stream().limit(limit).toList());
        });

        app.get("/api/templates/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<ExportTemplate> template = templateDatabase.findById("export_templates", id, ExportTemplate.class);
            if (template.isEmpty()) {
                ctx.status(404).json(new Document("error", "template not found"));
                return;
            }
            ctx.json(template.get());
        });

        app.put("/api/templates/{id}", ctx -> {
            String id = ctx.pathParam("id");
            ExportTemplate body = ctx.bodyAsClass(ExportTemplate.class);
            if (body.getContent() == null) {
                ctx.status(400).json(new Document("error", "content is required"));
                return;
            }

            ExportTemplate template = new ExportTemplate();
            template.setId(id);
            template.setName(body.getName() == null || body.getName().isBlank() ? id : body.getName());
            template.setContent(body.getContent());
            template.setUpdatedAt(Instant.now());
            templateDatabase.replaceById("export_templates", id, template);
            ctx.json(template);
        });

        app.post("/api/templates/seed", ctx -> {
            upsertTemplate(templateDatabase, "document-header", "Document Header", "\\\\documentclass[a4paper,11pt]{article}\\n\\\\usepackage[T1]{fontenc}\\n\\\\usepackage[utf8]{inputenc}\\n\\\\usepackage[german]{babel}\\n\\\\usepackage{hyperref}\\n\\\\usepackage{longtable}\\n\\\\usepackage{tikz}\\n\\\\title{${title}}\\n\\\\date{\\\\today}\\n\\\\begin{document}\\n\\\\maketitle\\n\\\\tableofcontents\\n\\\\newpage\\n");
            upsertTemplate(templateDatabase, "document-footer", "Document Footer", "\\\\end{document}\\n");
            upsertTemplate(templateDatabase, "speech-section", "Speech Section", "\\\\section{${groupType}: ${groupLabel}}\\n${speechBlock}\\n");
            upsertTemplate(templateDatabase, "speech-entry", "Speech Entry", "\\\\subsection{${speakerName}${factionSuffix}}\\n\\\\textbf{Rede-ID:} ${speechId}\\\\\\\\\\n\\\\textbf{Sitzung:} ${sessionId}\\\\\\\\\\n\\\\textbf{Tagesordnungspunkt:} ${agendaItem}\\\\\\\\\\n${startedAt}${endedAt}\\\\paragraph{NLP-Statistik}\\n${nlpStats}\\n\\\\paragraph{Redeinhalt}\\n${speechText}\\n${commentsBlock}\\n\\\\medskip\\\\hrule\\\\medskip\\n");
            upsertTemplate(templateDatabase, "comment-entry", "Comment Entry", "\\\\item [${commentMeta}] ${commentText}\\n");
            ctx.json(new Document("ok", true));
        });
    }

    /**
     * Inserts or replaces one template row.
     */
    private static void upsertTemplate(DatabaseHandler<ExportTemplate> db, String id, String name, String content) {
        ExportTemplate template = new ExportTemplate();
        template.setId(id);
        template.setName(name);
        template.setContent(content);
        template.setUpdatedAt(Instant.now());
        db.replaceById("export_templates", id, template);
    }

    /**
     * Parses an integer and falls back on invalid input.
     */
    private static int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
