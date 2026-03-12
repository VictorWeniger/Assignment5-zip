package org.example.model;

import java.time.Instant;

/**
 * @author
 * Victor Weniger
 */

/**
 * ExportTemplate data
 */
public class ExportTemplate implements Identifiable {
    private String id;
    private String name;
    private String content;
    private Instant updatedAt;

    @Override

/**
 * Getter
 */
    public String getId() {
        return id;
    }

/**
 * Setter
 */
    public void setId(String id) {
        this.id = id;
    }

/**
 * Getter
 */
    public String getName() {
        return name;
    }

/**
 * Setter
 */
    public void setName(String name) {
        this.name = name;
    }

/**
 * Getter
 */
    public String getContent() {
        return content;
    }

/**
 * Setter
 */
    public void setContent(String content) {
        this.content = content;
    }

/**
 * Getter
 */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

/**
 * Setter
 */
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
