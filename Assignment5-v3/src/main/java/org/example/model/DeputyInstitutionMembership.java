package org.example.model;

import java.time.LocalDate;

/**
 * @author
 * Victor Weniger
 */

/**
 * DeputyInstitutionMembership data
 */
public class DeputyInstitutionMembership {
    private String typeLabel;
    private String label;
    private LocalDate memberFrom;
    private LocalDate memberTo;
    private String functionLabel;
    private LocalDate functionFrom;
    private LocalDate functionTo;

/**
 * Getter
 */
    public String getTypeLabel() {
        return typeLabel;
    }

/**
 * Setter
 */
    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

/**
 * Getter
 */
    public String getLabel() {
        return label;
    }

/**
 * Setter
 */
    public void setLabel(String label) {
        this.label = label;
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
    public String getFunctionLabel() {
        return functionLabel;
    }

/**
 * Setter
 */
    public void setFunctionLabel(String functionLabel) {
        this.functionLabel = functionLabel;
    }

/**
 * Getter
 */
    public LocalDate getFunctionFrom() {
        return functionFrom;
    }

/**
 * Setter
 */
    public void setFunctionFrom(LocalDate functionFrom) {
        this.functionFrom = functionFrom;
    }

/**
 * Getter
 */
    public LocalDate getFunctionTo() {
        return functionTo;
    }

/**
 * Setter
 */
    public void setFunctionTo(LocalDate functionTo) {
        this.functionTo = functionTo;
    }
}
