package org.example.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author
 * Victor Weniger
 */

/**
 * CredentialsFileLoader config
 */
public final class CredentialsFileLoader {
    private CredentialsFileLoader() {
    }

/**
 * Method
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
        }
        return props;
    }
}
