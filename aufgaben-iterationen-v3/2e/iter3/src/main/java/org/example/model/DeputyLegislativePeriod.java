package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * Victor Weniger
 */

/**
 * DeputyLegislativePeriod data
 */
public class DeputyLegislativePeriod {
    private int legislativePeriod;
    private LocalDate memberFrom;
    private LocalDate memberTo;
    private String constituencyNumber;
    private String constituencyName;
    private String constituencyState;
    private String listName;
    private String mandateType;
    private final List<DeputyInstitutionMembership> institutions = new ArrayList<>();

/**
 * Getter
 */
    public int getLegislativePeriod() {
        return legislativePeriod;
    }

/**
 * Setter
 */
    public void setLegislativePeriod(int legislativePeriod) {
        this.legislativePeriod = legislativePeriod;
    }

/**
 * Getter
 */
    public LocalDate getMemberFrom() {
        return memberFrom;
    }

/**
 * Setter
 */
    public void setMemberFrom(LocalDate memberFrom) {
        this.memberFrom = memberFrom;
    }

/**
 * Getter
 */
    public LocalDate getMemberTo() {
        return memberTo;
    }

/**
 * Setter
 */
    public void setMemberTo(LocalDate memberTo) {
        this.memberTo = memberTo;
    }

/**
 * Getter
 */
    public String getConstituencyNumber() {
        return constituencyNumber;
    }

/**
 * Setter
 */
    public void setConstituencyNumber(String constituencyNumber) {
        this.constituencyNumber = constituencyNumber;
    }

/**
 * Getter
 */
    public String getConstituencyName() {
        return constituencyName;
    }

/**
 * Setter
 */
    public void setConstituencyName(String constituencyName) {
        this.constituencyName = constituencyName;
    }

/**
 * Getter
 */
    public String getConstituencyState() {
        return constituencyState;
    }

/**
 * Setter
 */
    public void setConstituencyState(String constituencyState) {
        this.constituencyState = constituencyState;
    }

/**
 * Getter
 */
    public String getListName() {
        return listName;
    }

/**
 * Setter
 */
    public void setListName(String listName) {
        this.listName = listName;
    }

/**
 * Getter
 */
    public String getMandateType() {
        return mandateType;
    }

/**
 * Setter
 */
    public void setMandateType(String mandateType) {
        this.mandateType = mandateType;
    }

/**
 * Getter
 */
    public List<DeputyInstitutionMembership> getInstitutions() {
        return institutions;
    }
}
