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
        NlpEngine duuiComposer = new DuuiComposerNlpEngine(config);
        NlpEngine duuiHttp = new DuuiNlpEngine(config);
        NlpEngine duui = duuiComposer.isAvailable() ? duuiComposer : duuiHttp;

        if (config == null) {
            LOGGER.warn("NLP engine config missing; DUUI is required for assignment mode.");
            return duui;
        }

        return switch (config.engineMode()) {
            case DUUI -> {
                if (!duui.isAvailable()) {
                    LOGGER.warn("NLP engine: DUUI (required), but DUUI endpoint or pipeline targets are missing.");
                }
                if (duui == duuiComposer) {
                    LOGGER.info("NLP engine: DUUI composer pipeline (spaCy + GerVader + ParlBERT)");
                } else {
                    LOGGER.info("NLP engine: DUUI HTTP endpoint");
                }
                yield duui;
            }
            case AUTO -> {
                if (duui.isAvailable()) {
                    if (duui == duuiComposer) {
                        LOGGER.info("NLP engine: DUUI composer pipeline (AUTO mode)");
                    } else {
                        LOGGER.info("NLP engine: DUUI HTTP endpoint (AUTO mode)");
                    }
                    yield duui;
                }
                LOGGER.warn("NLP engine: DUUI required, but DUUI endpoint is missing. AUTO mode will still use DUUI and fail fast.");
                yield duui;
            }
        };
    }
}
