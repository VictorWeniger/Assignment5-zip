package org.example.service.nlp;

import org.example.config.NlpConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpEngineSelector service
 */
public final class NlpEngineSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(NlpEngineSelector.class);

    private NlpEngineSelector() {
    }

/**
 * Method
 */
    public static NlpEngine select(NlpConfig config) {
        NlpEngine local = new LocalHeuristicNlpEngine();
        NlpEngine duui = new DuuiNlpEngine(config);

        if (config == null) {
            LOGGER.warn("NLP engine config missing; DUUI is required for assignment mode.");
            return duui;
        }

        return switch (config.engineMode()) {
            case LOCAL -> {
                LOGGER.info("NLP engine: LOCAL (configured)");
                yield local;
            }
            case DUUI -> {
                if (!duui.isAvailable()) {
                    LOGGER.warn("NLP engine: DUUI (required), but DUUI endpoint is missing. Configure `mpe.duui.endpoint`.");
                }
                LOGGER.info("NLP engine: DUUI");
                yield duui;
            }
            case AUTO -> {
                if (duui.isAvailable()) {
                    LOGGER.info("NLP engine: DUUI (AUTO mode)");
                    yield duui;
                }
                LOGGER.warn("NLP engine: DUUI required, but DUUI endpoint is missing. AUTO mode will still use DUUI and fail fast.");
                yield duui;
            }
        };
    }
}
