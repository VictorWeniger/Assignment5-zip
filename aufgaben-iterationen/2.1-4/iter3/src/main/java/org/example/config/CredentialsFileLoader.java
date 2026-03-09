package org.example.config;

/**
 * Developer guide: Loads the optional properties file used for runtime configuration overrides.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads key/value credentials from a properties file if present.
 */
public final class CredentialsFileLoader {
    private CredentialsFileLoader() {
    }

    /**
     * Reads a properties file from the provided path.
     *
     * @param path file path
     * @return loaded properties or an empty instance when unavailable
     */
    public static Properties load(String path) {
        Properties props = new Properties();
        if (path == null || path.isBlank()) {
            return props;
        }

        Path p = Path.of(path);
        if (!Files.exists(p)) {
            return props;
        }

        try (InputStream in = Files.newInputStream(p)) {
            props.load(in);
        } catch (IOException ignored) {
            // Best effort loading.
        }
        return props;
    }
}
