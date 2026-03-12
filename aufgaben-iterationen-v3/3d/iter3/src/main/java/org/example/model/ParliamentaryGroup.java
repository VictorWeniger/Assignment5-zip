package org.example.model;

import java.util.Objects;

/**
 * @author
 * Victor Weniger
 */

/**
 * ParliamentaryGroup data
 */
public class ParliamentaryGroup implements Identifiable {
    private String id;
    private String shortName;
    private String displayName;

/**
 * Constructor
 */
    public ParliamentaryGroup() {
    }

/**
 * Constructor
 */
    public ParliamentaryGroup(String id, String shortName, String displayName) {
        this.id = id;
        this.shortName = shortName;
        this.displayName = displayName;
    }

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
    public String getShortName() {
        return shortName;
    }

/**
 * Setter
 */
    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

/**
 * Getter
 */
    public String getDisplayName() {
        return displayName;
    }

/**
 * Setter
 */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override

/**
 * Method
 */
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

    @Override

/**
 * Method
 */
    public int hashCode() {
        return Objects.hash(id);
    }
}
