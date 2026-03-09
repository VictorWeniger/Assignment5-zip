package org.example.service.tex;

/**
 * Developer guide: Escapes user/content text safely for LaTeX rendering.
 */

/**
 * Escapes plain text for safe insertion into LaTeX content.
 */
public final class TeXEscaper {
    private TeXEscaper() {
    }

    /**
     * Escapes special LaTeX characters in the given value.
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\textbackslash{}")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("$", "\\$")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("^", "\\textasciicircum{}")
                .replace("~", "\\textasciitilde{}");
    }
}
