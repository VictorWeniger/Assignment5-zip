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

// [ITER2_EXPERIMENT_START]
// Experiment: temporary extraction point for retrospective iteration documentation.
// Will be removed/refined in iter3.
// [ITER2_EXPERIMENT_END]
