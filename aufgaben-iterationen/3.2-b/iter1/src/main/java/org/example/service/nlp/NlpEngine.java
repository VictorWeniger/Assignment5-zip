package org.example.service.nlp;

/**
 * Developer guide: Engine contract used by NLP orchestration (name, availability, annotate).
 */

import org.example.model.Speech;

/**
 * NLP engine contract for speech annotation providers.
 */
public interface NlpEngine {
    /**
     * Stable engine name used in metadata/logging.
     */
    String name();

    /**
     * Whether the engine can currently be used.
     */
    boolean isAvailable();

    /**
     * Annotates the given speech in-place.
     */
    void annotate(Speech speech);
}
