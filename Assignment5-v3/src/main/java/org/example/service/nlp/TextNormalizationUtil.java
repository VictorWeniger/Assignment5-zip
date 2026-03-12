package org.example.service.nlp;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * @author
 * Victor Weniger
 */

/**
 * TextNormalizationUtil service
 */
public final class TextNormalizationUtil {
    private TextNormalizationUtil() {
    }

/**
 * Method
 */
    public static String sanitizeSpeechText(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }

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

        String latin1ToUtf8 = new String(text.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
        int latinScore = qualityScore(latin1ToUtf8);
        if (latinScore > bestScore) {
            best = latin1ToUtf8;
            bestScore = latinScore;
        }

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
