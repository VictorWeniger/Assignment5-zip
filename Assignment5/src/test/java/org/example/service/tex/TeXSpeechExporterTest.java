package org.example.service.tex;

import org.example.model.Deputy;
import org.example.model.ParliamentaryGroup;
import org.example.model.Speech;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeXSpeechExporterTest {
    @Test
    void groupsExportByFactionWhenRequested() {
        Speech a = speech("s1", "20-42", "SPD");
        Speech b = speech("s2", "20-43", "CDU/CSU");

        String tex = new TeXSpeechExporter().exportSpeeches(
                "Test",
                List.of(a, b),
                Map.of(),
                "faction"
        );

        assertTrue(tex.contains("\\section{Fraktion: SPD}"));
        assertTrue(tex.contains("\\section{Fraktion: CDU/CSU}"));
    }

    @Test
    void includesTikzBlockWhenRequested() {
        Speech a = speech("s1", "20-42", "SPD");
        String tex = new TeXSpeechExporter().exportSpeeches(
                "Test",
                List.of(a),
                Map.of(),
                "protocol",
                true
        );
        assertTrue(tex.contains("\\begin{tikzpicture}"));
    }

    private Speech speech(String id, String protocol, String factionShort) {
        Speech speech = new Speech();
        speech.setId(id);
        speech.setProtocolId(protocol);
        speech.setText("Rede " + id);

        Deputy deputy = new Deputy();
        deputy.setFirstName("Vorname");
        deputy.setLastName("Nachname");
        ParliamentaryGroup group = new ParliamentaryGroup();
        group.setShortName(factionShort);
        deputy.setParliamentaryGroup(group);
        speech.setSpeaker(deputy);
        return speech;
    }
}
