package org.example.config;

/**
 * Developer guide: Resolves Mongo connection details including fallback source selection.
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MongoDB connection configuration.
 */
public class DatabaseConfig {
    private static final Path PROFESSOR_DB_CONFIG = Path.of("PPR_2025_G_11_03.txt");
    private final String connectionString;
    private final String databaseName;

    /**
     * Creates a database config object.
     */
    public DatabaseConfig(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
    }

    /**
     * MongoDB connection URI.
     */
    public String connectionString() {
        return connectionString;
    }

    /**
     * Database name to use inside MongoDB.
     */
    public String databaseName() {
        return databaseName;
    }

    /**
     * Reads connection settings from environment variables.
     */
    public static DatabaseConfig fromEnvironment() {
        String uri = System.getenv("MPE_MONGO_URI");
        String db = System.getenv("MPE_MONGO_DB");
        if (isNotBlank(uri) && isNotBlank(db)) {
            return new DatabaseConfig(uri, db);
        }

        if (Files.exists(PROFESSOR_DB_CONFIG)) {
            return fromProfessorConfig(PROFESSOR_DB_CONFIG);
        }

        throw new IllegalStateException("""
                MongoDB configuration is missing.
                Set MPE_MONGO_URI and MPE_MONGO_DB, or place the professor config file at PPR_2025_G_11_03.txt.
                """.trim());
    }

    private static DatabaseConfig fromProfessorConfig(Path path) {
        Map<String, String> values = new LinkedHashMap<>();
        try {
            for (String line : Files.readAllLines(path)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                String[] parts = trimmed.split("=", 2);
                values.put(parts[0].trim(), parts[1].trim());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read MongoDB config file: " + path, e);
        }

        String host = values.get("remote_host");
        String port = values.get("remote_port");
        String database = values.get("remote_database");
        String user = values.get("remote_user");
        String password = values.get("remote_password");

        if (!isNotBlank(host) || !isNotBlank(port) || !isNotBlank(database)
                || !isNotBlank(user) || !isNotBlank(password)) {
            throw new IllegalStateException("MongoDB config file is incomplete: " + path);
        }

        String uri = "mongodb://%s:%s@%s:%s/?authSource=%s".formatted(user, password, host, port, database);
        return new DatabaseConfig(uri, database);
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
