package org.example.config;

/**
 * Developer guide: NLP engine mode and DUUI endpoint/token/timeout settings.
 */

/**
 * Runtime configuration for NLP engine selection and DUUI endpoint access.
 */
public class NlpConfig {
    /**
     * NLP engine operating mode.
     */
    public enum EngineMode {
        LOCAL,
        DUUI,
        AUTO
    }

    private final EngineMode engineMode;
    private final String duuiEndpoint;
    private final String duuiAuthToken;
    private final int duuiTimeoutSeconds;
    private final String duuiMode;
    private final int duuiWorkers;
    private final String duuiSpacyTarget;
    private final String duuiGervaderTarget;
    private final String duuiParlbertTopicTarget;
    private final String duuiSpacyLanguage;
    private final String duuiSelection;
    private final String duuiViewName;

    /**
     * Creates a new NLP configuration object.
     */
    public NlpConfig(
            EngineMode engineMode,
            String duuiEndpoint,
            String duuiAuthToken,
            int duuiTimeoutSeconds,
            String duuiMode,
            int duuiWorkers,
            String duuiSpacyTarget,
            String duuiGervaderTarget,
            String duuiParlbertTopicTarget,
            String duuiSpacyLanguage,
            String duuiSelection,
            String duuiViewName
    ) {
        this.engineMode = engineMode;
        this.duuiEndpoint = duuiEndpoint;
        this.duuiAuthToken = duuiAuthToken;
        this.duuiTimeoutSeconds = duuiTimeoutSeconds;
        this.duuiMode = duuiMode;
        this.duuiWorkers = duuiWorkers;
        this.duuiSpacyTarget = duuiSpacyTarget;
        this.duuiGervaderTarget = duuiGervaderTarget;
        this.duuiParlbertTopicTarget = duuiParlbertTopicTarget;
        this.duuiSpacyLanguage = duuiSpacyLanguage;
        this.duuiSelection = duuiSelection;
        this.duuiViewName = duuiViewName;
    }

    /**
     * Selected NLP engine mode.
     */
    public EngineMode engineMode() {
        return engineMode;
    }

    /**
     * DUUI endpoint URL.
     */
    public String duuiEndpoint() {
        return duuiEndpoint;
    }

    /**
     * DUUI bearer token.
     */
    public String duuiAuthToken() {
        return duuiAuthToken;
    }

    /**
     * DUUI HTTP timeout in seconds.
     */
    public int duuiTimeoutSeconds() {
        return duuiTimeoutSeconds;
    }

    /**
     * DUUI mode (`remote`, `docker`, `mixed`).
     */
    public String duuiMode() {
        return duuiMode;
    }

    /**
     * Number of DUUI workers.
     */
    public int duuiWorkers() {
        return duuiWorkers;
    }

    /**
     * Target for the spaCy component (URL or docker image).
     */
    public String duuiSpacyTarget() {
        return duuiSpacyTarget;
    }

    /**
     * Target for the GerVader component (URL or docker image).
     */
    public String duuiGervaderTarget() {
        return duuiGervaderTarget;
    }

    /**
     * Target for the ParlBERT topic component (URL or docker image).
     */
    public String duuiParlbertTopicTarget() {
        return duuiParlbertTopicTarget;
    }

    /**
     * Optional language parameter for spaCy.
     */
    public String duuiSpacyLanguage() {
        return duuiSpacyLanguage;
    }

    /**
     * Routing selection field used by DUUI components.
     */
    public String duuiSelection() {
        return duuiSelection;
    }

    /**
     * UIMA view used for speech processing.
     */
    public String duuiViewName() {
        return duuiViewName;
    }

    /**
     * Whether a DUUI endpoint URL is configured.
     */
    public boolean hasDuuiEndpoint() {
        return duuiEndpoint != null && !duuiEndpoint.isBlank();
    }

    /**
     * Whether a DUUI authentication token is configured.
     */
    public boolean hasDuuiToken() {
        return duuiAuthToken != null && !duuiAuthToken.isBlank();
    }

    /**
     * Whether all mandatory DUUI component targets are configured.
     */
    public boolean hasDuuiPipelineTargets() {
        return notBlank(duuiSpacyTarget)
                && notBlank(duuiGervaderTarget)
                && notBlank(duuiParlbertTopicTarget);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
