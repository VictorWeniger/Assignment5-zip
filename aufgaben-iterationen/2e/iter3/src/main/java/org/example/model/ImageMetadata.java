package org.example.model;

/**
 * Developer guide: Image source/local path/mime/copyright metadata model.
 */

/**
 * Metadata for image assets (deputy pictures).
 */
public class ImageMetadata {
    private String sourceUrl;
    private String localPath;
    private String mimeType;
    private int width;
    private int height;
    private String copyrightNotice;

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
     * Returns MIME type.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets MIME type.
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Returns image width in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets image width in pixels.
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Returns image height in pixels.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets image height in pixels.
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Returns copyright notice.
     */
    public String getCopyrightNotice() {
        return copyrightNotice;
    }

    /**
     * Sets copyright notice.
     */
    public void setCopyrightNotice(String copyrightNotice) {
        this.copyrightNotice = copyrightNotice;
    }
}
