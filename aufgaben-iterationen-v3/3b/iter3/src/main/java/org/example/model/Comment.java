package org.example.model;

/**
 * @author
 * Victor Weniger
 */

/**
 * Comment data
 */
public class Comment implements Identifiable {
    private String id;
    private String speechId;
    private String authorName;
    private String authorFaction;
    private int speechOffset;
    private String text;

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
    public String getAuthorName() {
        return authorName;
    }

/**
 * Setter
 */
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

/**
 * Getter
 */
    public String getAuthorFaction() {
        return authorFaction;
    }

/**
 * Setter
 */
    public void setAuthorFaction(String authorFaction) {
        this.authorFaction = authorFaction;
    }

/**
 * Getter
 */
    public int getSpeechOffset() {
        return speechOffset;
    }

/**
 * Setter
 */
    public void setSpeechOffset(int speechOffset) {
        this.speechOffset = speechOffset;
    }

/**
 * Getter
 */
    public String getText() {
        return text;
    }

/**
 * Setter
 */
    public void setText(String text) {
        this.text = text;
    }
}
