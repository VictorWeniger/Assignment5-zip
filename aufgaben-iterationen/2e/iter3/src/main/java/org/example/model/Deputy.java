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
    private String birthPlace;
    private String birthCountry;
    private LocalDate deathDate;
    private String gender;
    private String maritalStatus;
    private String religion;
    private String profession;
    private String partyShort;
    private String vitaShort;
    private String publicationRequiredInfo;
    private ParliamentaryGroup parliamentaryGroup;
    private DeputyRole role = DeputyRole.DEPUTY;
    private final List<DeputyLegislativePeriod> legislativePeriods = new ArrayList<>();
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

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getBirthCountry() {
        return birthCountry;
    }

    public void setBirthCountry(String birthCountry) {
        this.birthCountry = birthCountry;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getProfession() {
        return profession;
    }

    public void setProfession(String profession) {
        this.profession = profession;
    }

    public String getPartyShort() {
        return partyShort;
    }

    public void setPartyShort(String partyShort) {
        this.partyShort = partyShort;
    }

    public String getVitaShort() {
        return vitaShort;
    }

    public void setVitaShort(String vitaShort) {
        this.vitaShort = vitaShort;
    }

    public String getPublicationRequiredInfo() {
        return publicationRequiredInfo;
    }

    public void setPublicationRequiredInfo(String publicationRequiredInfo) {
        this.publicationRequiredInfo = publicationRequiredInfo;
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

    public List<DeputyLegislativePeriod> getLegislativePeriods() {
        return legislativePeriods;
    }

    /**
     * Returns mutable image metadata list.
     */
    public List<ImageMetadata> getImages() {
        return images;
    }
}
