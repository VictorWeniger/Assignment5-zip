package org.example.api;

/**
 * Developer guide: DTO for candidate agenda items that likely have reusable video coverage.
 */

/**
 * Suggested protocol agenda item candidate for targeted video work.
 */
public record AgendaVideoCandidate(
        String protocolId,
        String sessionId,
        int legislativePeriod,
        int sessionNumber,
        int agendaItem,
        String agendaLabel,
        long speechCount,
        int speakerCount,
        String speechesUrl
) {
}
