package org.example.model;

/**
 * Developer guide: Domain model linking one speech to local/remote video resources.
 */

/**
 * Video metadata linked to one speech.
 */
public class SpeechVideo implements Identifiable {
    private String id;
    private String speechId;
    private String sourceUrl;
    private String videoPageUrl;
    private String embedUrl;
    private String streamUrl;
    private String localPath;
    private int durationSeconds;

    /**
     * Returns video id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets video id.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns referenced speech id.
     */
    public String getSpeechId() {
        return speechId;
    }

    /**
     * Sets referenced speech id.
     */
    public void setSpeechId(String speechId) {
        this.speechId = speechId;
    }

    /**
     * Returns video source URL.
     */
    public String getSourceUrl() {
        return sourceUrl;
    }

    /**
     * Sets video source URL.
     */
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    /**
     * Returns Bundestag video page URL.
     */
    public String getVideoPageUrl() {
        return videoPageUrl;
    }

    /**
     * Sets Bundestag video page URL.
     */
    public void setVideoPageUrl(String videoPageUrl) {
        this.videoPageUrl = videoPageUrl;
    }

    /**
     * Returns embeddable Bundestag player URL.
     */
    public String getEmbedUrl() {
        return embedUrl;
    }

    /**
     * Sets embeddable Bundestag player URL.
     */
    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

    /**
     * Returns direct media stream URL if available.
     */
    public String getStreamUrl() {
        return streamUrl;
    }

    /**
     * Sets direct media stream URL if available.
     */
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    /**
     * Returns local file path.
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * Sets local file path.
     */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    /**
     * Returns duration in seconds.
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Sets duration in seconds.
     */
    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
