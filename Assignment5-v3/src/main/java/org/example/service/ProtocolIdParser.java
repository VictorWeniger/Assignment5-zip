package org.example.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolIdParser service
 */
public final class ProtocolIdParser {
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("(\\d{2})(\\d{3})(?:[^0-9]|$)", Pattern.CASE_INSENSITIVE);

    private ProtocolIdParser() {
    }

/**
 * Method
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
 * ParsedProtocolId service
 */
    public record ParsedProtocolId(String protocolId, int legislativePeriod, int sessionNumber) {
    }
}
