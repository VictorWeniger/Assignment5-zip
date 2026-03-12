package org.example.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author
 * Victor Weniger
 */

/**
 * BundestagProtocolDownloader service
 */
public class BundestagProtocolDownloader {
    private static final String OPEN_DATA_URL = "https://www.bundestag.de/services/opendata";
    private static final String AJAX_FILTERLIST_PATH = "/ajax/filterlist/";
    private static final int PAGE_SIZE = 10;
    private static final Pattern XML_URL_PATTERN = Pattern.compile("https?://[^\\s\"'<>]+?\\.xml(?:\\?[^\\s\"'<>]*)?", Pattern.CASE_INSENSITIVE);

    private final HttpClient httpClient = HttpClient.newHttpClient();

/**
 * Method
 */
    public List<String> fetchProtocolXmlLinks() throws IOException {
        String html = get(OPEN_DATA_URL);
        Set<String> links = new LinkedHashSet<>();
        collectProtocolLinksFromOpenDataPage(html, links);
        collectInlineXmlUrls(html, links);
        return new ArrayList<>(links);
    }

/**
 * Method
 */
    public String downloadXml(String url) throws IOException {
        return get(url);
    }

    private String get(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Assignment5Importer/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode() + " for " + url);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while fetching " + url, e);
        }
    }

    private void collectProtocolLinksFromOpenDataPage(String html, Set<String> links) throws IOException {
        Document document = Jsoup.parse(html, OPEN_DATA_URL);
        Set<String> moduleUrls = new LinkedHashSet<>();

        for (Element element : document.select("[data-dataloader-url], [data-ajax-url], [data-url]")) {
            String candidate = firstNonBlank(
                    element.attr("data-dataloader-url"),
                    element.attr("data-ajax-url"),
                    element.attr("data-url")
            );
            if (candidate.isBlank()) {
                continue;
            }
            String resolved = document.baseUri().isBlank()
                    ? URI.create(OPEN_DATA_URL).resolve(candidate).toString()
                    : URI.create(document.baseUri()).resolve(candidate).toString();
            if (resolved.contains(AJAX_FILTERLIST_PATH) && looksLikeProtocolSection(element)) {
                moduleUrls.add(resolved);
            }
        }

        for (String moduleUrl : moduleUrls) {
            collectProtocolLinksFromModule(moduleUrl, links);
        }
    }

    private void collectInlineXmlUrls(String html, Set<String> links) {
        Matcher matcher = XML_URL_PATTERN.matcher(html);
        while (matcher.find()) {
            addIfXmlLink(matcher.group(), links);
        }
    }

    private void collectProtocolLinksFromModule(String moduleUrl, Set<String> links) throws IOException {
        int offset = 0;
        int stableRounds = 0;

        while (stableRounds < 2) {
            String pagedUrl = buildPagedUrl(moduleUrl, offset);
            String html = get(pagedUrl);
            Document document = Jsoup.parse(html, pagedUrl);
            int before = links.size();
            collectXmlAnchors(document, links);

            if (links.size() == before) {
                stableRounds++;
            } else {
                stableRounds = 0;
            }

            if (!hasMoreResults(document, offset, links.size() - before)) {
                break;
            }
            offset += PAGE_SIZE;
        }
    }

    private void collectXmlAnchors(Document document, Set<String> links) {
        Elements anchors = document.select("a[href]");
        for (Element anchor : anchors) {
            addIfXmlLink(anchor.attr("abs:href"), links);
            addIfXmlLink(anchor.attr("href"), links);
        }
    }

    private boolean hasMoreResults(Document document, int currentOffset, int newlyFoundLinks) {
        if (newlyFoundLinks <= 0) {
            return false;
        }

        for (Element element : document.select("[data-total], [data-count], [data-results-count], [data-hits]")) {
            Integer total = parseInteger(firstNonBlank(
                    element.attr("data-total"),
                    element.attr("data-count"),
                    element.attr("data-results-count"),
                    element.attr("data-hits")
            ));
            if (total != null) {
                return currentOffset + PAGE_SIZE < total;
            }
        }

        String text = document.text();
        Matcher matcher = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)").matcher(text);
        if (matcher.find()) {
            int page = Integer.parseInt(matcher.group(1));
            int totalPages = Integer.parseInt(matcher.group(2));
            return page < totalPages;
        }

        return newlyFoundLinks >= PAGE_SIZE;
    }

    private String buildPagedUrl(String moduleUrl, int offset) {
        String separator = moduleUrl.contains("?") ? "&" : "?";
        return moduleUrl + separator + "limit=" + PAGE_SIZE + "&offset=" + offset;
    }

    private boolean looksLikeProtocolSection(Element element) {
        for (Element candidate : element.parents()) {
            String text = candidate.text().toLowerCase();
            if (text.contains("plenarprotokolle")) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void addIfXmlLink(String href, Set<String> links) {
        if (href == null || href.isBlank()) {
            return;
        }
        String resolved = URI.create(OPEN_DATA_URL).resolve(href).toString();
        String lower = resolved.toLowerCase();
        if (!lower.contains(".xml")) {
            return;
        }
        if (!links.contains(resolved)) {
            links.add(resolved);
        }
    }
}
