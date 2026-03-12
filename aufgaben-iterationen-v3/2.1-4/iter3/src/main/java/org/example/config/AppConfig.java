package org.example.config;

import java.time.Duration;
import java.util.Properties;

/**
 * @author
 * Victor Weniger
 */

/**
 * AppConfig config
 */
public class AppConfig {
    private final int port;
    private final Duration importInterval;
    private final DatabaseConfig databaseConfig;
    private final boolean downloadMediaAssets;
    private final String mediaDirectory;
    private final NlpConfig nlpConfig;

/**
 * Constructor
 */
    public AppConfig(
            int port,
            Duration importInterval,
            DatabaseConfig databaseConfig,
            boolean downloadMediaAssets,
            String mediaDirectory,
            NlpConfig nlpConfig
    ) {
        this.port = port;
        this.importInterval = importInterval;
        this.databaseConfig = databaseConfig;
        this.downloadMediaAssets = downloadMediaAssets;
        this.mediaDirectory = mediaDirectory;
        this.nlpConfig = nlpConfig;
    }

/**
 * Method
 */
    public int port() {
        return port;
    }

/**
 * Method
 */
    public Duration importInterval() {
        return importInterval;
    }

/**
 * Method
 */
    public DatabaseConfig databaseConfig() {
        return databaseConfig;
    }

/**
 * Method
 */
    public boolean downloadMediaAssets() {
        return downloadMediaAssets;
    }

/**
 * Method
 */
    public String mediaDirectory() {
        return mediaDirectory;
    }

/**
 * Method
 */
    public NlpConfig nlpConfig() {
        return nlpConfig;
    }

/**
 * Method
 */
    public static AppConfig fromEnvironment() {
        String credentialsPath = System.getenv().getOrDefault("MPE_CREDENTIALS_FILE", ".credentials/mpe.properties");
        Properties props = CredentialsFileLoader.load(credentialsPath);

        int port = Integer.parseInt(System.getenv().getOrDefault("MPE_PORT", "7070"));
        long intervalMinutes = Long.parseLong(System.getenv().getOrDefault("MPE_IMPORT_INTERVAL_MINUTES", "120"));
        boolean downloadMediaAssets = Boolean.parseBoolean(System.getenv().getOrDefault("MPE_DOWNLOAD_MEDIA", "false"));
        String mediaDirectory = System.getenv().getOrDefault("MPE_MEDIA_DIR", "data/media");
        String duuiEndpoint = getenvOrProps("MPE_DUUI_ENDPOINT", "mpe.duui.endpoint", "", props);
        String duuiToken = getenvOrProps("MPE_DUUI_TOKEN", "mpe.duui.token", "", props);
        int duuiTimeout = Integer.parseInt(getenvOrProps("MPE_DUUI_TIMEOUT_SECONDS", "mpe.duui.timeoutSeconds", "30", props));
        String duuiMode = getenvOrProps("MPE_DUUI_MODE", "duui.mode", "remote", props);
        int duuiWorkers = Integer.parseInt(getenvOrProps("MPE_DUUI_WORKERS", "duui.workers", "1", props));
        String duuiSpacyTarget = getenvOrProps("MPE_DUUI_SPACY_TARGET", "duui.spacy.target", "", props);
        String duuiGervaderTarget = getenvOrProps("MPE_DUUI_GERVADER_TARGET", "duui.gervader.target", "", props);
        String duuiParlbertTopicTarget = getenvOrProps("MPE_DUUI_PARLBERT_TOPIC_TARGET", "duui.parlbert_topic.target", "", props);
        String duuiSpacyLanguage = getenvOrProps("MPE_DUUI_SPACY_LANGUAGE", "duui.spacy.param.language", "de", props);
        String duuiSelection = getenvOrProps("MPE_DUUI_SELECTION", "duui.param.selection", "sentences", props);
        String duuiViewName = getenvOrProps("MPE_DUUI_VIEW", "duui.param.view", "speech", props);

        String modeRaw = getenvOrProps("MPE_NLP_ENGINE", "mpe.nlp.engine", "duui", props);
        NlpConfig.EngineMode engineMode = parseEngineMode(modeRaw);

        return new AppConfig(
                port,
                Duration.ofMinutes(intervalMinutes),
                DatabaseConfig.fromEnvironment(),
                downloadMediaAssets,
                mediaDirectory,
                new NlpConfig(
                        engineMode,
                        duuiEndpoint,
                        duuiToken,
                        duuiTimeout,
                        duuiMode,
                        Math.max(1, duuiWorkers),
                        duuiSpacyTarget,
                        duuiGervaderTarget,
                        duuiParlbertTopicTarget,
                        duuiSpacyLanguage,
                        duuiSelection,
                        duuiViewName
                )
        );
    }

    private static String getenvOrProps(String env, String prop, String fallback, Properties properties) {
        String envValue = System.getenv(env);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        String propValue = properties.getProperty(prop);
        if (propValue != null && !propValue.isBlank()) {
            return propValue;
        }
        return fallback;
    }

    private static NlpConfig.EngineMode parseEngineMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return NlpConfig.EngineMode.AUTO;
        }
        String normalized = raw.trim().toUpperCase();
        try {
            return NlpConfig.EngineMode.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return NlpConfig.EngineMode.AUTO;
        }
    }
}
