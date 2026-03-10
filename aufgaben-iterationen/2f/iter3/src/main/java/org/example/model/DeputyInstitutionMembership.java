package org.example.model;

import java.time.LocalDate;

/**
 * Institution membership metadata for one deputy within a legislative period.
 */
public class DeputyInstitutionMembership {
    private String typeLabel;
    private String label;
    private LocalDate memberFrom;
    private LocalDate memberTo;
    private String functionLabel;
    private LocalDate functionFrom;
    private LocalDate functionTo;

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    public String getFunctionLabel() {
        return functionLabel;
    }

    public void setFunctionLabel(String functionLabel) {
        this.functionLabel = functionLabel;
    }

    public LocalDate getFunctionFrom() {
        return functionFrom;
    }

    public void setFunctionFrom(LocalDate functionFrom) {
        this.functionFrom = functionFrom;
    }

    public LocalDate getFunctionTo() {
        return functionTo;
    }

    public void setFunctionTo(LocalDate functionTo) {
        this.functionTo = functionTo;
    }
}
