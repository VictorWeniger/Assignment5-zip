package org.example.api;

/**
 * Developer guide: Serves Swagger UI/OpenAPI resources.
 */

import io.javalin.Javalin;
import io.javalin.http.ContentType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves OpenAPI YAML and embedded Swagger UI pages.
 */
public final class SwaggerController {
    private SwaggerController() {
    }

    /**
     * Registers Swagger/OpenAPI routes.
     */
    public static void register(Javalin app) {
        app.get("/swagger/openapi.yaml", ctx -> {
            String yaml = readClasspathResource("openapi.yaml");
            ctx.contentType("application/yaml");
            ctx.result(yaml);
        });

        app.get("/swagger", ctx -> {
            ctx.contentType(ContentType.HTML);
            ctx.result(swaggerHtml());
        });
    }

    private static String readClasspathResource(String path) {
        try (InputStream input = SwaggerController.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                return "openapi: 3.0.3\ninfo:\n  title: Missing OpenAPI spec\n  version: 0.0.0\npaths: {}\n";
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "openapi: 3.0.3\ninfo:\n  title: Error loading OpenAPI spec\n  version: 0.0.0\npaths: {}\n";
        }
    }

    private static String swaggerHtml() {
        return """
                <!doctype html>
                <html lang=\"en\">
                <head>
                  <meta charset=\"utf-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>MPE Swagger</title>
                  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\" />
                  <style>
                    body { margin: 0; background: #f5f7fb; }
                    #swagger-ui { max-width: 1200px; margin: 0 auto; }
                  </style>
                </head>
                <body>
                  <div id=\"swagger-ui\"></div>
                  <script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>
                  <script>
                    window.ui = SwaggerUIBundle({
                      url: '/swagger/openapi.yaml',
                      dom_id: '#swagger-ui',
                      deepLinking: true,
                      presets: [SwaggerUIBundle.presets.apis],
                      layout: 'BaseLayout'
                    });
                  </script>
                </body>
                </html>
                """;
    }
}
