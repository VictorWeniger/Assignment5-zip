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
    public String getBirthPlace() {
        return birthPlace;
    }

/**
 * Setter
 */
    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

/**
 * Getter
 */
    public String getBirthCountry() {
        return birthCountry;
    }

/**
 * Setter
 */
    public void setBirthCountry(String birthCountry) {
        this.birthCountry = birthCountry;
    }

/**
 * Getter
 */
    public LocalDate getDeathDate() {
        return deathDate;
    }

/**
 * Setter
 */
    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

/**
 * Getter
 */
    public String getGender() {
        return gender;
    }

/**
 * Setter
 */
    public void setGender(String gender) {
        this.gender = gender;
    }

/**
 * Getter
 */
    public String getMaritalStatus() {
        return maritalStatus;
    }

/**
 * Setter
 */
    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

/**
 * Getter
 */
    public String getReligion() {
        return religion;
    }

/**
 * Setter
 */
    public void setReligion(String religion) {
        this.religion = religion;
    }

/**
 * Getter
 */
    public String getProfession() {
        return profession;
    }

/**
 * Setter
 */
    public void setProfession(String profession) {
        this.profession = profession;
    }

/**
 * Getter
 */
    public String getPartyShort() {
        return partyShort;
    }

/**
 * Setter
 */
    public void setPartyShort(String partyShort) {
        this.partyShort = partyShort;
    }

/**
 * Getter
 */
    public String getVitaShort() {
        return vitaShort;
    }

/**
 * Setter
 */
    public void setVitaShort(String vitaShort) {
        this.vitaShort = vitaShort;
    }

/**
 * Getter
 */
    public String getPublicationRequiredInfo() {
        return publicationRequiredInfo;
    }

/**
 * Setter
 */
    public void setPublicationRequiredInfo(String publicationRequiredInfo) {
        this.publicationRequiredInfo = publicationRequiredInfo;
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
    public List<DeputyLegislativePeriod> getLegislativePeriods() {
        return legislativePeriods;
    }

/**
 * Getter
 */
    public List<ImageMetadata> getImages() {
        return images;
    }
}
