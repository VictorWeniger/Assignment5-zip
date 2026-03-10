package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Legislative-period metadata for one deputy.
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

    public int getLegislativePeriod() {
        return legislativePeriod;
    }

    public void setLegislativePeriod(int legislativePeriod) {
        this.legislativePeriod = legislativePeriod;
    }

    public LocalDate getMemberFrom() {
        return memberFrom;
    }

    public void setMemberFrom(LocalDate memberFrom) {
        this.memberFrom = memberFrom;
    }

    public LocalDate getMemberTo() {
        return memberTo;
    }

    public void setMemberTo(LocalDate memberTo) {
        this.memberTo = memberTo;
    }

    public String getConstituencyNumber() {
        return constituencyNumber;
    }

    public void setConstituencyNumber(String constituencyNumber) {
        this.constituencyNumber = constituencyNumber;
    }

    public String getConstituencyName() {
        return constituencyName;
    }

    public void setConstituencyName(String constituencyName) {
        this.constituencyName = constituencyName;
    }

    public String getConstituencyState() {
        return constituencyState;
    }

    public void setConstituencyState(String constituencyState) {
        this.constituencyState = constituencyState;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public String getMandateType() {
        return mandateType;
    }

    public void setMandateType(String mandateType) {
        this.mandateType = mandateType;
    }

    public List<DeputyInstitutionMembership> getInstitutions() {
        return institutions;
    }
}
