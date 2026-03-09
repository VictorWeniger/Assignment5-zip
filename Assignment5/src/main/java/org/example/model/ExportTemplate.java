package org.example.model;

/**
 * Developer guide: Persisted export template fragment used by TeX/PDF generation.
 */

import java.time.Instant;

/**
 * Stored export template snippet used by TeX export rendering.
 */
public class ExportTemplate implements Identifiable {
    private String id;
    private String name;
    private String content;
    private Instant updatedAt;

    /**
     * Returns template id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets template id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns template display name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets template display name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns template content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets template content.
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Returns update timestamp.
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets update timestamp.
     */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
