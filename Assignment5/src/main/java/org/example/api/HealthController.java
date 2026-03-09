package org.example.api;

/**
 * Developer guide: Provides health-check endpoint used for service liveness checks.
 */

import io.javalin.Javalin;

/**
 * Registers a simple liveness endpoint.
 */
public final class HealthController {
    private HealthController() {
    }

    /**
     * Registers `/health`.
     */
    public static void register(Javalin app) {
        app.get("/health", ctx -> ctx.json("ok"));
    }
}
