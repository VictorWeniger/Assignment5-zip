package org.example.api;

import io.javalin.Javalin;

import java.util.List;
import java.util.Map;

/**
 * @author
 * Victor Weniger
 */

/**
 * ApiDocsController controller
 */
public final class ApiDocsController {
    private ApiDocsController() {
    }

/**
 * Method
 */
    public static void register(Javalin app) {
        app.get("/api/docs", ctx -> ctx.json(Map.of(
                "message", "Route overview (temporary until Swagger UI integration)",
                "routes", List.of(
                        "GET /health",
                        "GET /api/import/preview?period=20&limit=3&force=false",
                        "POST /api/import/run?period=20&limit=3&force=false",
                        "POST /api/import/run/{protocolId}?force=false",
                        "POST /api/nlp/run?limit=300&force=false",
                        "POST /api/nlp/run/{speechId}?force=false",
                        "GET /api/nlp/stats",
                        "POST /api/nlp/import?path=/abs/or/relative/file.xmi.gz&createMissing=false",
                        "POST /api/nlp/import?path=/abs/or/relative/xmi-directory&createMissing=false",
                        "GET /api/protocols?includeRaw=false",
                        "GET /api/protocols/{id}?includeRaw=false",
                        "GET /api/sessions?protocolId=...",
                        "GET /api/speeches?protocolId=...&protocolIds=...&sessionId=...&agendaItem=...&speakerId=...&faction=...&topic=...&matchMode=and|or&from=...&to=...&limit=...",
                        "GET /api/speeches/{id}",
                        "GET /api/speeches/{id}/detail",
                        "GET /api/speeches/search?q=...",
                        "GET /api/deputies?limit=...",
                        "GET /api/deputies/{id}",
                        "GET /api/topics?limit=100",
                        "GET /api/videos?speechId=...&limit=...",
                        "GET /api/videos/{id}",
                        "GET /api/export/tex?protocolId=...&protocolIds=...&speakerId=...&faction=...&topic=...&matchMode=and|or&groupBy=protocol|speaker|faction|topic|none&from=...&to=...&includeTikz=true&limit=...",
                        "GET /api/export/pdf?protocolId=...&protocolIds=...&speakerId=...&faction=...&topic=...&matchMode=and|or&groupBy=protocol|speaker|faction|topic|none&from=...&to=...&includeTikz=true&limit=...",
                        "GET /api/templates",
                        "GET /api/templates/{id}",
                        "PUT /api/templates/{id}",
                        "POST /api/templates/seed",
                        "GET /api/stats"
                )
        )));
    }
}
