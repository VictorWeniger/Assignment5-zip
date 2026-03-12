package org.example.service.tex;

/**
 * @author
 * Victor Weniger
 */

/**
 * TeXEscaper service
 */
public final class TeXEscaper {
    private TeXEscaper() {
    }

/**
 * Method
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
