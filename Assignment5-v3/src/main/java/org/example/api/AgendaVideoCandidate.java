package org.example.api;

/**
 * @author
 * Victor Weniger
 */

/**
 * AgendaVideoCandidate controller
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
