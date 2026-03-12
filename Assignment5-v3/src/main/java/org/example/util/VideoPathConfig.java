package org.example.util;

import java.nio.file.Path;

/**
 * @author
 * Victor Weniger
 */

/**
 * VideoPathConfig utility
 */
public final class VideoPathConfig {
    private static final String ENV_BUNDLED_VIDEO_DIR = "MPE_BUNDLED_VIDEO_DIR";
    private static final String DEFAULT_BUNDLED_VIDEO_DIR = ".local-data/bundestag-videos";

    private VideoPathConfig() {
    }

/**
 * Method
 */
    public static Path bundledVideoRoot() {
        String value = System.getenv().getOrDefault(ENV_BUNDLED_VIDEO_DIR, DEFAULT_BUNDLED_VIDEO_DIR);
        if (value == null || value.isBlank()) {
            value = DEFAULT_BUNDLED_VIDEO_DIR;
        }
        return Path.of(value);
    }

/**
 * Method
 */
    public static boolean fileNameMatchesVideoId(String fileName, String videoId) {
        if (fileName == null || fileName.isBlank() || videoId == null || videoId.isBlank()) {
            return false;
        }
        int idx = fileName.indexOf(videoId);
        while (idx >= 0) {
            int before = idx - 1;
            int after = idx + videoId.length();
            boolean beforeOk = before < 0 || !Character.isDigit(fileName.charAt(before));
            boolean afterOk = after >= fileName.length() || !Character.isDigit(fileName.charAt(after));
            if (beforeOk && afterOk) {
                return true;
            }
            idx = fileName.indexOf(videoId, idx + 1);
        }
        return false;
    }
}
