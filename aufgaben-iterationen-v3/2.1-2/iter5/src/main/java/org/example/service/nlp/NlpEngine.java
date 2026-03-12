package org.example.service.nlp;

import org.example.model.Speech;

/**
 * @author
 * Victor Weniger
 */

/**
 * NlpEngine service
 */
public interface NlpEngine {
    String name();

    boolean isAvailable();

    void annotate(Speech speech);
}
