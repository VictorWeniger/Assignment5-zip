package org.example.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolSession data
 */
public class ProtocolSession implements Identifiable {
    private String id;
    private String protocolId;
    private int legislativePeriod;
    private int sessionNumber;
    private LocalDate sessionDate;
    private final List<String> agenda = new ArrayList<>();
    private final List<String> speechIds = new ArrayList<>();

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
    public String getProtocolId() {
        return protocolId;
    }

/**
 * Setter
 */
    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

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
    public int getSessionNumber() {
        return sessionNumber;
    }

/**
 * Setter
 */
    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

/**
 * Getter
 */
    public LocalDate getSessionDate() {
        return sessionDate;
    }

/**
 * Setter
 */
    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

/**
 * Getter
 */
    public List<String> getAgenda() {
        return agenda;
    }

/**
 * Getter
 */
    public List<String> getSpeechIds() {
        return speechIds;
    }
}
