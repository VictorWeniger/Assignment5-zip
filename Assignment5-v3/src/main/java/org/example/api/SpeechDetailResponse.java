package org.example.api;

import org.example.model.Deputy;
import org.example.model.Speech;
import org.example.model.SpeechVideo;

/**
 * @author
 * Victor Weniger
 */

/**
 * SpeechDetailResponse controller
 */
public record SpeechDetailResponse(
        Speech speech,
        Deputy speaker,
        SpeechVideo video,
        Integer clipStartSeconds,
        Integer clipEndSeconds
) {
}
