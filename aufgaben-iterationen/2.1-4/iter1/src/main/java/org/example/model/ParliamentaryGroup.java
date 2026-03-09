package org.example.model;

/**
 * Developer guide: Domain model for parliamentary group metadata.
 */

import java.util.Objects;

/**
 * Parliamentary group metadata for deputies and speech aggregation.
 */
public class ParliamentaryGroup implements Identifiable {
    private String id;
    private String shortName;
    private String displayName;

    /**
     * Creates an empty group instance.
     */
    public ParliamentaryGroup() {
    }

    /**
     * Creates a group instance with all fields.
     */
    public ParliamentaryGroup(String id, String shortName, String displayName) {
        this.id = id;
        this.shortName = shortName;
        this.displayName = displayName;
    }

    /**
     * Returns the group id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the group id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the short group label.
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Sets the short group label.
     */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    /**
     * Returns the display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name.
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Equality based on id.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParliamentaryGroup that = (ParliamentaryGroup) o;
        return Objects.equals(id, that.id);
    }

    /**
     * Hash code based on id.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
