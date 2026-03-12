package org.example.model;

import java.time.Instant;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolDocument data
 */
public class ProtocolDocument implements Identifiable {
    private String id;
    private int legislativePeriod;
    private int sessionNumber;
    private String sourceUrl;
    private String rawXml;
    private Instant importedAt;

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
    public String getSourceUrl() {
        return sourceUrl;
    }

/**
 * Setter
 */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

/**
 * Getter
 */
    public String getRawXml() {
        return rawXml;
    }

/**
 * Setter
 */
    public void setRawXml(String rawXml) {
        this.rawXml = rawXml;
    }

/**
 * Getter
 */
    public Instant getImportedAt() {
        return importedAt;
    }

/**
 * Setter
 */
    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
