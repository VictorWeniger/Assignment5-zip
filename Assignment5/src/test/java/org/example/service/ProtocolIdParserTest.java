package org.example.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtocolIdParserTest {
    @Test
    void parsesProtocolIdFromXmlUrl() {
        ProtocolIdParser.ParsedProtocolId parsed = ProtocolIdParser.parse("https://example.org/plenarprotokolle/20042.xml");
        assertEquals("20-42", parsed.protocolId());
        assertEquals(20, parsed.legislativePeriod());
        assertEquals(42, parsed.sessionNumber());
    }

    @Test
    void parsesProtocolIdFromXmlUrlWithQueryString() {
        ProtocolIdParser.ParsedProtocolId parsed = ProtocolIdParser.parse("https://example.org/plenarprotokolle/20042.xml?download=1");
        assertEquals("20-42", parsed.protocolId());
        assertEquals(20, parsed.legislativePeriod());
        assertEquals(42, parsed.sessionNumber());
    }

    @Test
    void parsesProtocolIdFromBundestagStyleDownloadPath() {
        ProtocolIdParser.ParsedProtocolId parsed = ProtocolIdParser.parse("https://www.bundestag.de/resource/blob/123456/20042-data.xml");
        assertEquals("20-42", parsed.protocolId());
        assertEquals(20, parsed.legislativePeriod());
        assertEquals(42, parsed.sessionNumber());
    }

    @Test
    void throwsOnNonMatchingUrl() {
        assertThrows(IllegalArgumentException.class, () -> ProtocolIdParser.parse("https://example.org/file.txt"));
    }
}
