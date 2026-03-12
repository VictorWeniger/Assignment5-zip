package org.example.service;

import org.example.model.ImageMetadata;
import org.example.model.SpeechVideo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author
 * Victor Weniger
 */

/**
 * MediaAssetDownloadService service
 */
public class MediaAssetDownloadService {
    private final boolean enabled;
    private final Path basePath;
    private final HttpClient httpClient;

/**
 * Constructor
 */
    public MediaAssetDownloadService(boolean enabled, String baseDirectory) {
        this.enabled = enabled;
        this.basePath = Path.of(baseDirectory);
        this.httpClient = HttpClient.newHttpClient();
    }

/**
 * Method
 */
    public void downloadSpeechVideo(SpeechVideo speechVideo) {
        if (!enabled || speechVideo == null || isBlank(speechVideo.getSourceUrl())) {
            return;
        }

        String extension = extensionFromUrl(speechVideo.getSourceUrl(), ".mp4");
        Path destination = basePath.resolve("videos").resolve(speechVideo.getId() + extension);
        if (download(speechVideo.getSourceUrl(), destination)) {
            speechVideo.setLocalPath(destination.toString());
        }
    }

/**
 * Method
 */
    public String downloadSharedVideo(String sourceUrl, String fileStem) {
        if (!enabled || isBlank(sourceUrl) || isBlank(fileStem)) {
            return null;
        }

        String extension = extensionFromUrl(sourceUrl, ".mp4");
        Path destination = basePath.resolve("videos").resolve(fileStem + extension);
        if (download(sourceUrl, destination)) {
            return destination.toString();
        }
        return null;
    }

/**
 * Method
 */
    public void downloadDeputyImage(ImageMetadata imageMetadata, String deputyId) {
        if (!enabled || imageMetadata == null || isBlank(imageMetadata.getSourceUrl()) || isBlank(deputyId)) {
            return;
        }

        String extension = extensionFromUrl(imageMetadata.getSourceUrl(), ".jpg");
        Path destination = basePath.resolve("images").resolve(deputyId + extension);
        if (download(imageMetadata.getSourceUrl(), destination)) {
            imageMetadata.setLocalPath(destination.toString());
        }
    }

    private boolean download(String sourceUrl, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl)).GET().build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return false;
            }

            try (InputStream input = response.body()) {
                Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException | IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String extensionFromUrl(String url, String fallback) {
        int q = url.indexOf('?');
        String clean = q >= 0 ? url.substring(0, q) : url;
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot < clean.lastIndexOf('/')) {
            return fallback;
        }
        String ext = clean.substring(dot);
        return ext.length() > 8 ? fallback : ext;
    }
}
