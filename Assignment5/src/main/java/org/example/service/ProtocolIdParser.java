package org.example.service;

/**
 * Developer guide: Parses/normalizes protocol ID formats used by import logic.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses protocol metadata from Bundestag XML file URLs.
 */
public final class ProtocolIdParser {
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("(\\d{2})(\\d{3})(?:[^0-9]|$)", Pattern.CASE_INSENSITIVE);

    private ProtocolIdParser() {
    }

    /**
     * Parses protocol id, legislative period, and session number from an XML URL.
     */
    public static ParsedProtocolId parse(String xmlUrl) {
        Matcher matcher = PROTOCOL_PATTERN.matcher(xmlUrl);
        ParsedProtocolId bestMatch = null;
        while (matcher.find()) {
            int legislativePeriod = Integer.parseInt(matcher.group(1));
            int sessionNumber = Integer.parseInt(matcher.group(2));
            bestMatch = new ParsedProtocolId(legislativePeriod + "-" + sessionNumber, legislativePeriod, sessionNumber);
        }
        if (bestMatch == null) {
            throw new IllegalArgumentException("Could not parse legislative period/session from URL: " + xmlUrl);
        }
        return bestMatch;
    }

    /**
     * Parsed protocol identifier components.
     */
    public record ParsedProtocolId(String protocolId, int legislativePeriod, int sessionNumber) {
    }
}
