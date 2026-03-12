package org.example.model;

/**
 * @author
 * Victor Weniger
 */

/**
 * SpeechVideo data
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
    public String getSpeechId() {
        return speechId;
    }

/**
 * Setter
 */
    public void setSpeechId(String speechId) {
        this.speechId = speechId;
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
    public String getVideoPageUrl() {
        return videoPageUrl;
    }

/**
 * Setter
 */
    public void setVideoPageUrl(String videoPageUrl) {
        this.videoPageUrl = videoPageUrl;
    }

/**
 * Getter
 */
    public String getEmbedUrl() {
        return embedUrl;
    }

/**
 * Setter
 */
    public void setEmbedUrl(String embedUrl) {
        this.embedUrl = embedUrl;
    }

/**
 * Getter
 */
    public String getStreamUrl() {
        return streamUrl;
    }

/**
 * Setter
 */
    public void setStreamUrl(String streamUrl) {
        this.streamUrl = streamUrl;
    }

/**
 * Getter
 */
    public String getLocalPath() {
        return localPath;
    }

/**
 * Setter
 */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

/**
 * Getter
 */
    public int getDurationSeconds() {
        return durationSeconds;
    }

/**
 * Setter
 */
    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
