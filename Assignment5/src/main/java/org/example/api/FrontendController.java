package org.example.api;

/**
 * Developer guide: Registers server-rendered frontend routes and passes route params to templates.
 */

import io.javalin.Javalin;

import java.util.HashMap;
import java.util.Map;

/**
 * Registers server-rendered frontend page routes.
 */
public final class FrontendController {
    private FrontendController() {
    }

    /**
     * Registers main page routes and forwards selected query parameters into template models.
     */
    public static void register(Javalin app) {
        app.get("/", ctx -> ctx.render("templates/index.ftl", model("Multimodal Parliament Explorer")));

        app.get("/protocols", ctx -> ctx.render("templates/protocols.ftl", model("Protocols")));

        app.get("/protocol/{id}", ctx -> {
            Map<String, Object> model = model("Protocol Detail");
            model.put("protocolId", ctx.pathParam("id"));
            ctx.render("templates/protocol-detail.ftl", model);
        });

        app.get("/speeches", ctx -> {
            Map<String, Object> model = model("Speeches");
            String protocolId = ctx.queryParam("protocolId");
            if (protocolId != null && !protocolId.isBlank()) {
                model.put("protocolId", protocolId);
            }
            ctx.render("templates/speeches.ftl", model);
        });

        app.get("/analytics", ctx -> {
            Map<String, Object> model = model("Analytics");
            String protocolId = ctx.queryParam("protocolId");
            if (protocolId != null && !protocolId.isBlank()) {
                model.put("protocolId", protocolId);
            }
            String speechId = ctx.queryParam("speechId");
            if (speechId != null && !speechId.isBlank()) {
                model.put("speechId", speechId);
            }
            ctx.render("templates/analytics.ftl", model);
        });

        app.get("/speech/{id}", ctx -> {
            Map<String, Object> model = model("Speech Detail");
            model.put("speechId", ctx.pathParam("id"));
            ctx.render("templates/speech-detail.ftl", model);
        });

        app.get("/export", ctx -> {
            Map<String, Object> model = model("Export (TeX)");
            String protocolId = ctx.queryParam("protocolId");
            if (protocolId != null && !protocolId.isBlank()) {
                model.put("protocolId", protocolId);
            }
            ctx.render("templates/export.ftl", model);
        });
    }

    private static Map<String, Object> model(String title) {
        Map<String, Object> model = new HashMap<>();
        model.put("title", title);
        return model;
    }
}
