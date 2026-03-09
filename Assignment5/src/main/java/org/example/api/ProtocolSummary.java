package org.example.api;

/**
 * Developer guide: API projection type for reduced protocol payloads.
 */

import org.example.model.ProtocolDocument;

import java.time.Instant;

/**
 * Reduced protocol projection for list and detail responses without raw XML payload.
 */
public record ProtocolSummary(
        String id,
        int legislativePeriod,
        int sessionNumber,
        String sourceUrl,
        Instant importedAt
) {
    /**
     * Creates a summary view from a full protocol document.
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
