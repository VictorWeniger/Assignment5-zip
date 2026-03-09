package org.example.model;

/**
 * Developer guide: Domain model for interjections/comments attached to a speech.
 */

/**
 * Inline comment/interjection associated with a speech.
 */
public class Comment implements Identifiable {
    private String id;
    private String speechId;
    private String authorName;
    private String authorFaction;
    private int speechOffset;
    private String text;

    /**
     * Returns comment id.
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Sets comment id.
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
     * Returns comment author name.
     */
    public String getAuthorName() {
        return authorName;
    }

    /**
     * Sets comment author name.
     */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    /**
     * Returns comment author faction.
     */
    public String getAuthorFaction() {
        return authorFaction;
    }

    /**
     * Sets comment author faction.
     */
    public void setAuthorFaction(String authorFaction) {
        this.authorFaction = authorFaction;
    }

    /**
     * Returns speech text offset.
     */
    public int getSpeechOffset() {
        return speechOffset;
    }

    /**
     * Sets speech text offset.
     */
    public void setSpeechOffset(int speechOffset) {
        this.speechOffset = speechOffset;
    }

    /**
     * Returns comment text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets comment text.
     */
    public void setText(String text) {
        this.text = text;
    }
}
