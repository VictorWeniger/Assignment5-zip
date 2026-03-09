package org.example.model;

/**
 * Developer guide: Domain model for one session and its agenda entries.
 */

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * One parliamentary session with agenda and referenced speech ids.
 */
public class ProtocolSession implements Identifiable {
    private String id;
    private String protocolId;
    private int legislativePeriod;
    private int sessionNumber;
    private LocalDate sessionDate;
    private final List<String> agenda = new ArrayList<>();
    private final List<String> speechIds = new ArrayList<>();

    /**
     * Returns session id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets session id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns protocol id.
     */
    public String getProtocolId() {
        return protocolId;
    }

    /**
     * Sets protocol id.
     */
    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    /**
     * Returns legislative period.
     */
    public int getLegislativePeriod() {
        return legislativePeriod;
    }

    /**
     * Sets legislative period.
     */
    public void setLegislativePeriod(int legislativePeriod) {
        this.legislativePeriod = legislativePeriod;
    }

    /**
     * Returns session number.
     */
    public int getSessionNumber() {
        return sessionNumber;
    }

    /**
     * Sets session number.
     */
    public void setSessionNumber(int sessionNumber) {
        this.sessionNumber = sessionNumber;
    }

    /**
     * Returns session date.
     */
    public LocalDate getSessionDate() {
        return sessionDate;
    }

    /**
     * Sets session date.
     */
    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    /**
     * Returns mutable agenda item list.
     */
    public List<String> getAgenda() {
        return agenda;
    }

    /**
     * Returns mutable speech id list.
     */
    public List<String> getSpeechIds() {
        return speechIds;
    }
}
