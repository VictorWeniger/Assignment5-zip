package org.example.api;

import org.example.model.ProtocolDocument;

import java.time.Instant;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolSummary controller
 */
public record ProtocolSummary(
        String id,
        int legislativePeriod,
        int sessionNumber,
        String sourceUrl,
        Instant importedAt
) {

/**
 * Method
 */
    public static ProtocolSummary from(ProtocolDocument protocol) {
        return new ProtocolSummary(
                protocol.getId(),
                protocol.getLegislativePeriod(),
                protocol.getSessionNumber(),
                protocol.getSourceUrl(),
                protocol.getImportedAt()
        );
    }
}
