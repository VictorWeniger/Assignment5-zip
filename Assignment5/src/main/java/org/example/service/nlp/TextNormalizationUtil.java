package org.example.service.nlp;

/**
 * Developer guide: Repairs common text encoding artifacts and normalizes whitespace before NLP/timing.
 */

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Utility helpers for normalizing speech text before NLP/timestamp processing.
 */
public final class TextNormalizationUtil {
    private TextNormalizationUtil() {
    }

    /**
     * Repairs likely mojibake and normalizes line endings/whitespace.
     */
    public static String sanitizeSpeechText(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }

        // Normalize obvious formatting issues first so scoring/re-encoding sees stable input.
        String normalized = text
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u00A0', ' ');

        String repaired = repairLikelyMojibake(normalized);
        repaired = repairCommonMojibakeFragments(repaired);
        return repaired
                .replaceAll("[\\t\\f\\x0B]+", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private static String repairLikelyMojibake(String text) {
        String best = text;
        int bestScore = qualityScore(text);

        // Common case: UTF-8 bytes were interpreted as ISO-8859-1.
        String latin1ToUtf8 = new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        int latinScore = qualityScore(latin1ToUtf8);
        if (latinScore > bestScore) {
            best = latin1ToUtf8;
            bestScore = latinScore;
        }

        // Secondary case: UTF-8 bytes were interpreted as Windows-1252.
        String cp1252ToUtf8 = new String(text.getBytes(java.nio.charset.Charset.forName("windows-1252")), StandardCharsets.UTF_8);
        int cpScore = qualityScore(cp1252ToUtf8);
        if (cpScore > bestScore) {
            best = cp1252ToUtf8;
        }
        return best;
    }

    private static int qualityScore(String text) {
        if (text == null || text.isBlank()) {
            return Integer.MIN_VALUE / 2;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        // Penalize classic mojibake fragments and reward valid German umlauts.
        int mojibake = count(lower, "ã")
                + count(lower, "â€")
                + count(lower, "â€“")
                + count(lower, "â€œ")
                + count(lower, "â€ž")
                + count(lower, "�");
        int umlauts = count(lower, "ä")
                + count(lower, "ö")
                + count(lower, "ü")
                + count(lower, "ß");
        return (umlauts * 2) - (mojibake * 4);
    }

    private static int count(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += Math.max(1, needle.length());
        }
        return count;
    }

    private static String repairCommonMojibakeFragments(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String out = text;
        // Latin-1 interpreted UTF-8 (German umlauts + punctuation)
        out = out.replace("Ã„", "Ä")
                .replace("Ã–", "Ö")
                .replace("Ãœ", "Ü")
                .replace("Ã¤", "ä")
                .replace("Ã¶", "ö")
                .replace("Ã¼", "ü")
                .replace("ÃŸ", "ß")
                .replace("â€“", "–")
                .replace("â€”", "—")
                .replace("â€ž", "„")
                .replace("â€œ", "“")
                .replace("â€", "”")
                .replace("â€˜", "‘")
                .replace("â€™", "’")
                .replace("â€¦", "…")
                .replace("Â§", "§")
                .replace("Â°", "°")
                .replace("Â·", "·")
                .replace("Â", "");
        return out;
    }
}
