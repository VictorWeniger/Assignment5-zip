package org.example.config;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpConfig config
 */
public class NlpConfig {

/**
 * EngineMode config
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
 * Constructor
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
 * Method
 */
    public EngineMode engineMode() {
        return engineMode;
    }

/**
 * Method
 */
    public String duuiEndpoint() {
        return duuiEndpoint;
    }

/**
 * Method
 */
    public String duuiAuthToken() {
        return duuiAuthToken;
    }

/**
 * Method
 */
    public int duuiTimeoutSeconds() {
        return duuiTimeoutSeconds;
    }

/**
 * Method
 */
    public String duuiMode() {
        return duuiMode;
    }

/**
 * Method
 */
    public int duuiWorkers() {
        return duuiWorkers;
    }

/**
 * Method
 */
    public String duuiSpacyTarget() {
        return duuiSpacyTarget;
    }

/**
 * Method
 */
    public String duuiGervaderTarget() {
        return duuiGervaderTarget;
    }

/**
 * Method
 */
    public String duuiParlbertTopicTarget() {
        return duuiParlbertTopicTarget;
    }

/**
 * Method
 */
    public String duuiSpacyLanguage() {
        return duuiSpacyLanguage;
    }

/**
 * Method
 */
    public String duuiSelection() {
        return duuiSelection;
    }

/**
 * Method
 */
    public String duuiViewName() {
        return duuiViewName;
    }

/**
 * Method
 */
    public boolean hasDuuiEndpoint() {
        return duuiEndpoint != null && !duuiEndpoint.isBlank();
    }

/**
 * Method
 */
    public boolean hasDuuiToken() {
        return duuiAuthToken != null && !duuiAuthToken.isBlank();
    }

/**
 * Method
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
