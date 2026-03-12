package org.example.api;

import io.javalin.Javalin;

/**
 * @author
 * Victor Weniger
 */

/**
 * HealthController controller
 */
public final class HealthController {
    private HealthController() {
    }

/**
 * Method
 */
    public static void register(Javalin app) {
        app.get("/health", ctx -> ctx.json("ok"));
    }
}
