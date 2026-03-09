package org.example.api;

/**
 * Developer guide: Response envelope combining speech, speaker, video, and optional clip window.
 */

import org.example.model.Deputy;
import org.example.model.Speech;
import org.example.model.SpeechVideo;

/**
 * Combined response payload for speech detail page data.
 */
public record SpeechDetailResponse(
        Speech speech,
        Deputy speaker,
        SpeechVideo video,
        Integer clipStartSeconds,
        Integer clipEndSeconds
) {
}
