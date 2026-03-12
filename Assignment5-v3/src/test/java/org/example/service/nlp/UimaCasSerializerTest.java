package org.example.service.nlp;

import org.example.model.Comment;
import org.example.model.Speech;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UimaCasSerializerTest {
    @Test
    void enrichesSpeechWithCompactMetadataByDefault() {
        Speech speech = new Speech();
        speech.setId("s-1");
        speech.setProtocolId("20-42");
        speech.setSessionId("20-42-1");
        speech.setText("Berlin ist gut.");
        speech.setTopics(List.of(Map.of("label", "Climate", "score", 2)));
        speech.setNamedEntities(List.of(Map.of("type", "LOC", "text", "Berlin", "begin", 0, "end", 6)));
        speech.setPosDistribution(Map.of("NOUN", 1));
        speech.setNlp(new HashMap<>());
        speech.getNlp().put("sentenceSentiments", List.of(Map.of("begin", 0, "end", 15, "score", 0.5)));
        speech.getNlp().put("coreferences", List.of(Map.of(
                "label", "berlin",
                "mentions", List.of(Map.of("begin", 0, "end", 6))
        )));

        Comment comment = new Comment();
        comment.setText("Zwischenruf");
        comment.setAuthorName("Max");
        comment.setSpeechOffset(7);
        speech.getComments().add(comment);

        UimaCasSerializer.enrichSpeechWithUimaCas(speech);

        assertTrue(speech.getNlp().containsKey("uimaTypeSystem"));
        assertTrue(speech.getNlp().containsKey("uimaSummary"));
        assertFalse(speech.getNlp().containsKey("uimaCas"));
        assertTrue(speech.getNlp().containsKey("uimaSerializedAt"));
    }

    @Test
    void canStillStoreFullCasWhenExplicitlyEnabled() {
        Speech speech = new Speech();
        speech.setId("s-2");
        speech.setText("Berlin ist gut.");
        speech.setNlp(new HashMap<>());

        UimaCasSerializer.enrichSpeechWithUimaCas(speech, true);

        assertTrue(speech.getNlp().containsKey("uimaTypeSystem"));
        assertTrue(speech.getNlp().containsKey("uimaSummary"));
        assertTrue(speech.getNlp().containsKey("uimaCas"));
    }
}
