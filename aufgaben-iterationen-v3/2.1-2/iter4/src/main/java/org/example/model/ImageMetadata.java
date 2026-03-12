package org.example.model;

/**
 * @author
 * Victor Weniger
 */

/**
 * ImageMetadata data
 */
public class ImageMetadata {
    private String sourceUrl;
    private String localPath;
    private String mimeType;
    private int width;
    private int height;
    private String copyrightNotice;

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
    public String getMimeType() {
        return mimeType;
    }

/**
 * Setter
 */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

/**
 * Getter
 */
    public int getWidth() {
        return width;
    }

/**
 * Setter
 */
    public void setWidth(int width) {
        this.width = width;
    }

/**
 * Getter
 */
    public int getHeight() {
        return height;
    }

/**
 * Setter
 */
    public void setHeight(int height) {
        this.height = height;
    }

/**
 * Getter
 */
    public String getCopyrightNotice() {
        return copyrightNotice;
    }

/**
 * Setter
 */
    public void setCopyrightNotice(String copyrightNotice) {
        this.copyrightNotice = copyrightNotice;
    }
}
