package org.example.model;

/**
 * Developer guide: Shared interface for entities with string IDs.
 */

/**
 * Marker interface for entities with a stable string id.
 */
public interface Identifiable {
    /**
     * Returns the entity identifier.
     */
    String getId();
}
