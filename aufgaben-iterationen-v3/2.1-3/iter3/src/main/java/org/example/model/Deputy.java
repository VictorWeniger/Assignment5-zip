package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * Victor Weniger
 */

/**
 * Deputy data
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
    public String getFirstName() {
        return firstName;
    }

/**
 * Setter
 */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

/**
 * Getter
 */
    public String getLastName() {
        return lastName;
    }

/**
 * Setter
 */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

/**
 * Getter
 */
    public String getTitle() {
        return title;
    }

/**
 * Setter
 */
    public void setTitle(String title) {
        this.title = title;
    }

/**
 * Getter
 */
    public LocalDate getBirthDate() {
        return birthDate;
    }

/**
 * Setter
 */
    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

/**
 * Getter
 */
    public ParliamentaryGroup getParliamentaryGroup() {
        return parliamentaryGroup;
    }

/**
 * Setter
 */
    public void setParliamentaryGroup(ParliamentaryGroup parliamentaryGroup) {
        this.parliamentaryGroup = parliamentaryGroup;
    }

/**
 * Getter
 */
    public DeputyRole getRole() {
        return role;
    }

/**
 * Setter
 */
    public void setRole(DeputyRole role) {
        this.role = role;
    }

/**
 * Getter
 */
    public List<ImageMetadata> getImages() {
        return images;
    }
}
