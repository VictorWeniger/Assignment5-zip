package org.example.model;

/**
 * Developer guide: Domain model for imported protocol root document.
 */

import java.time.Instant;

/**
 * Raw imported protocol document and import metadata.
 */
public class ProtocolDocument implements Identifiable {
    private String id;
    private int legislativePeriod;
    private int sessionNumber;
    private String sourceUrl;
    private String rawXml;
    private Instant importedAt;

    /**
     * Returns protocol id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets protocol id.
     */
    public void setId(String id) {
        this.id = id;
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
     * Returns source URL.
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Sets source URL.
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
     * Returns raw XML content.
     */
    public String getRawXml() {
        return rawXml;
    }

    /**
     * Sets raw XML content.
     */
    public void setRawXml(String rawXml) {
        this.rawXml = rawXml;
    }

    /**
     * Returns import timestamp.
     */
    public Instant getImportedAt() {
        return importedAt;
    }

    /**
     * Sets import timestamp.
     */
    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
