package org.example.service.tex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeXEscaperTest {
    @Test
    void escapesSpecialCharacters() {
        String escaped = TeXEscaper.escape("a_b%$#{}&~^");
        assertTrue(escaped.contains("\\_"));
        assertTrue(escaped.contains("\\%"));
        assertTrue(escaped.contains("\\$"));
        assertTrue(escaped.contains("\\#"));
        assertTrue(escaped.contains("\\{"));
        assertTrue(escaped.contains("\\}"));
        assertTrue(escaped.contains("\\&"));
        assertTrue(escaped.contains("\\textasciitilde{}"));
        assertTrue(escaped.contains("\\textasciicircum{}"));
    }
}
