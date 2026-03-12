package org.example.service.nlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextNormalizationUtilTest {
    @Test
    void repairsCommonMojibakeFragments() {
        String input = "Sehr geehrte Frau PrÃ¤sidentin! FÃ¼r Europa â€“ und das Thema â€žMigrationâ€œ.";
        String out = TextNormalizationUtil.sanitizeSpeechText(input);

        assertTrue(out.contains("Präsidentin"), out);
        assertTrue(out.contains("Für"), out);
        assertTrue(out.contains("–"), out);
        assertTrue(out.contains("„Migration“"), out);
    }
}
