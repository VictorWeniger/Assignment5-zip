package org.example.service;

/**
 * Developer guide: Resolves and enriches deputy portrait images from Bundestag sources.
 */

import org.example.model.Deputy;
import org.example.model.ImageMetadata;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeputyImageEnrichmentService {
    private static final String IMAGE_DB_SEARCH = "https://bilddatenbank.bundestag.de/search/picture-result";
    private static final String PORTRAIT_FILTER = "Porträt/Portrait";
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MultimodalParliamentExplorer/1.0)";

    public void enrichWithProfileImage(Deputy deputy) {
        if (deputy == null || deputy.getId() == null || deputy.getId().isBlank()) {
            return;
        }
        if (!deputy.getImages().isEmpty()) {
            return;
        }

        String nameQuery = (safe(deputy.getFirstName()) + " " + safe(deputy.getLastName())).trim();
        if (nameQuery.isBlank()) {
            return;
        }

        try {
            for (String candidateQuery : candidateQueries(deputy)) {
                for (String url : candidateSearchUrls(candidateQuery)) {
                    String searchHtml = get(url);
                    String source = resolveImageUrlFromSearchOrDetail(searchHtml, deputy);
                    if (!source.isBlank()) {
                        ImageMetadata metadata = new ImageMetadata();
                        metadata.setSourceUrl(source);
                        metadata.setMimeType(inferMimeType(source));
                        metadata.setCopyrightNotice("Deutscher Bundestag");
                        deputy.getImages().add(metadata);
                        return;
                    }
                }
            }
        } catch (IOException ignored) {
            // Best effort loading only.
        }
    }

    public Map<String, Object> debugProfileImageSearch(Deputy deputy) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (deputy == null) {
            out.put("error", "deputy is null");
            return out;
        }
        out.put("deputyId", deputy.getId());
        out.put("firstName", deputy.getFirstName());
        out.put("lastName", deputy.getLastName());
        out.put("queries", candidateQueries(deputy));

        java.util.List<Map<String, Object>> attempts = new java.util.ArrayList<>();
        out.put("attempts", attempts);

        try {
            for (String candidateQuery : candidateQueries(deputy)) {
                for (String url : candidateSearchUrls(candidateQuery)) {
                    Map<String, Object> attempt = new LinkedHashMap<>();
                    attempt.put("query", candidateQuery);
                    attempt.put("searchUrl", url);
                    try {
                        String searchHtml = get(url);
                        Document searchDoc = Jsoup.parse(searchHtml, IMAGE_DB_SEARCH);
                        int imageCount = searchDoc.select("img[src]").size();
                        int detailLinkCount = searchDoc.select("a[href*='picture-detail?id=']").size();
                        attempt.put("searchHtmlLength", searchHtml.length());
                        attempt.put("imgCount", imageCount);
                        attempt.put("detailLinkCount", detailLinkCount);

                        String directImage = resolveImageUrl(searchDoc);
                        attempt.put("directImageUrl", directImage);

                        Element firstDetail = searchDoc.select("a[href*='picture-detail?id=']").first();
                        if (firstDetail != null) {
                            String detailUrl = firstDetail.absUrl("href");
                            attempt.put("firstDetailUrl", detailUrl);
                            String detailHtml = get(detailUrl);
                            Document detailDoc = Jsoup.parse(detailHtml, detailUrl);
                            attempt.put("detailHtmlLength", detailHtml.length());
                            attempt.put("detailResolvedImageUrl", resolveImageUrl(detailDoc));
                        }
                    } catch (Exception ex) {
                        attempt.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                    attempts.add(attempt);
                }
            }
        } catch (Exception ex) {
            out.put("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        return out;
    }

    private String resolveImageUrlFromSearchOrDetail(String searchHtml, Deputy deputy) throws IOException {
        Document searchDoc = Jsoup.parse(searchHtml, IMAGE_DB_SEARCH);

        String expectedLastName = safe(deputy.getLastName()).toLowerCase();
        Element firstCandidate = null;
        for (Element link : searchDoc.select("a[href*='picture-detail?id=']")) {
            String href = link.absUrl("href");
            if (href.isBlank()) {
                continue;
            }
            if (firstCandidate == null) {
                firstCandidate = link;
            }
            String text = (link.text() + " " + link.parent().text()).toLowerCase();
            if (!expectedLastName.isBlank() && text.contains(expectedLastName)) {
                return toHttps(resolveImageUrlFromDetailPage(href));
            }
        }

        if (firstCandidate != null) {
            String href = firstCandidate.absUrl("href");
            if (!href.isBlank()) {
                return toHttps(resolveImageUrlFromDetailPage(href));
            }
        }

        // Last resort only when no detail page link exists at all.
        return toHttps(resolveImageUrl(searchDoc));
    }

    private String resolveImageUrlFromDetailPage(String detailUrl) throws IOException {
        String detailHtml = get(detailUrl);
        Document detailDoc = Jsoup.parse(detailHtml, detailUrl);
        return resolveImageUrl(detailDoc);
    }

    private String resolveImageUrl(Document doc) {
        for (Element meta : doc.select("meta[property=og:image], meta[name=twitter:image], meta[itemprop=image]")) {
            String content = absoluteFromElement(meta, "content");
            if (isImageUrl(content)) {
                return content;
            }
        }

        for (Element link : doc.select("a[href]")) {
            for (String value : candidateImageValues(link)) {
                if (isImageUrl(value)) {
                    return value;
                }
            }
        }

        for (Element img : doc.select("img[src], img[data-src], img[data-original], img[data-lazy-src], img[srcset], img[data-srcset]")) {
            for (String value : candidateImageValues(img)) {
                if (isImageUrl(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private boolean isImageUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String lower = value.trim().toLowerCase();
        if (lower.contains("matomo") || lower.contains("statistik.bundestag.de") || lower.contains("/piwik") || lower.contains("tracking")) {
            return false;
        }

        if (lower.contains("/includes/images/layout/") || lower.contains("dummy_16_9")) {
            return false;
        }

        try {
            URI uri = URI.create(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
            if ((host.contains("bundestag.de") || host.contains("bilddatenbank.bundestag.de"))
                    && (path.contains("/fotos/") || path.contains("/resource/image/"))) {
                return true;
            }
        } catch (Exception ignored) {
            // best-effort below
        }

        String noQuery = lower.split("\\?", 2)[0];
        if (noQuery.endsWith(".jpg") || noQuery.endsWith(".jpeg") || noQuery.endsWith(".png") || noQuery.endsWith(".webp")) {
            return !(lower.contains("logo") || lower.contains("icon") || lower.contains("sprite") || noQuery.endsWith(".svg"));
        }

        String nestedUrl = extractNestedUrlParam(value);
        if (nestedUrl != null && !nestedUrl.equals(value)) {
            return isImageUrl(nestedUrl);
        }

        if (lower.contains("logo") || lower.contains("icon") || lower.contains("sprite") || lower.endsWith(".svg")) {
            return false;
        }
        return false;
    }

    private java.util.List<String> candidateQueries(Deputy deputy) {
        String firstName = safe(deputy.getFirstName());
        String lastName = safe(deputy.getLastName());
        java.util.List<String> queries = new java.util.ArrayList<>();
        if (!lastName.isBlank() && !firstName.isBlank()) {
            queries.add(firstName + " " + lastName);
            queries.add(lastName + ", " + firstName);
        }
        return queries;
    }

    private java.util.List<String> candidateSearchUrls(String candidateQuery) {
        String encodedQuery = URLEncoder.encode(candidateQuery, StandardCharsets.UTF_8);
        String encodedLower = URLEncoder.encode(candidateQuery.toLowerCase(), StandardCharsets.UTF_8);
        String portraitFilter = URLEncoder.encode(PORTRAIT_FILTER, StandardCharsets.UTF_8);
        java.util.List<String> urls = new java.util.ArrayList<>();
        urls.add(IMAGE_DB_SEARCH + "?query=" + encodedLower);
        urls.add(IMAGE_DB_SEARCH + "?query=" + encodedQuery);
        urls.add(IMAGE_DB_SEARCH
                + "?query=" + encodedQuery
                + "&filterQuery%5Bereignis%5D%5B0%5D=" + portraitFilter
                + "&sortVal=3");
        urls.add(IMAGE_DB_SEARCH
                + "?query="
                + "&filterQuery%5Bereignis%5D%5B0%5D=" + portraitFilter
                + "&sortVal=3");
        urls.add(IMAGE_DB_SEARCH
                + "?query="
                + "&filterQuery%5Bname%5D%5B%5D=" + encodedQuery
                + "&filterQuery%5Bereignis%5D%5B0%5D=" + portraitFilter
                + "&sortVal=3");
        return urls;
    }

    private String get(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer(IMAGE_DB_SEARCH)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
                .timeout(15_000)
                .ignoreHttpErrors(false)
                .get()
                .outerHtml();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String inferMimeType(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private String toHttps(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        if (url.startsWith("http://bilddatenbank.bundestag.de/")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private List<String> candidateImageValues(Element element) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        for (String attr : List.of("href", "src", "data-src", "data-original", "data-lazy-src", "content")) {
            String value = absoluteFromElement(element, attr);
            if (value != null && !value.isBlank()) {
                values.add(value);
            }
        }
        for (String attr : List.of("srcset", "data-srcset")) {
            String raw = element.attr(attr);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            for (String part : raw.split(",")) {
                String candidate = part.trim().split("\\s+")[0];
                if (candidate.isBlank()) {
                    continue;
                }
                String abs = toAbsoluteUrl(element, candidate);
                if (abs != null && !abs.isBlank()) {
                    values.add(abs);
                }
            }
        }
        return values;
    }

    private String absoluteFromElement(Element element, String attr) {
        if (element == null || attr == null) {
            return "";
        }
        String direct = element.attr(attr);
        if (direct == null || direct.isBlank()) {
            return "";
        }
        String abs = element.absUrl(attr);
        if (abs != null && !abs.isBlank()) {
            return abs;
        }
        return toAbsoluteUrl(element, direct);
    }

    private String toAbsoluteUrl(Element element, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            URI base = URI.create(element.baseUri());
            return base.resolve(value).toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private String extractNestedUrlParam(String value) {
        try {
            URI uri = URI.create(value);
            String query = uri.getQuery();
            if (query == null || query.isBlank()) {
                return null;
            }
            for (String token : query.split("&")) {
                String[] parts = token.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                String key = parts[0].toLowerCase();
                if (!key.equals("url") && !key.equals("src") && !key.equals("image")) {
                    continue;
                }
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
