package org.example.model;

/**
 * Developer guide: Domain model for a deputy including party/group and image metadata.
 */

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Deputy profile including role, faction, and image metadata.
 */
public class Deputy implements Identifiable {
    private String id;
    private String firstName;
    private String lastName;
    private String title;
    private LocalDate birthDate;
    private ParliamentaryGroup parliamentaryGroup;
    private DeputyRole role = DeputyRole.DEPUTY;
    private final List<ImageMetadata> images = new ArrayList<>();

    /**
     * Returns the deputy id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets the deputy id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the first name.
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name.
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the last name.
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the last name.
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the honorific title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the honorific title.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the birth date.
     */
    public LocalDate getBirthDate() {
        return birthDate;
    }

    /**
     * Sets the birth date.
     */
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Returns the parliamentary group.
     */
    public ParliamentaryGroup getParliamentaryGroup() {
        return parliamentaryGroup;
    }

    /**
     * Sets the parliamentary group.
     */
    public void setParliamentaryGroup(ParliamentaryGroup parliamentaryGroup) {
        this.parliamentaryGroup = parliamentaryGroup;
    }

    /**
     * Returns the deputy role.
     */
    public DeputyRole getRole() {
        return role;
    }

    /**
     * Sets the deputy role.
     */
    public void setRole(DeputyRole role) {
        this.role = role;
    }

    /**
     * Returns mutable image metadata list.
     */
    public List<ImageMetadata> getImages() {
        return images;
    }
}
