package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.ProtocolSession;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.example.util.VideoPathConfig;

/**
 * @author
 * Victor Weniger
 */

/**
 * AgendaVideoImportService service
 */
public class AgendaVideoImportService {
    private static final Pattern MEDIA_URL_PATTERN = Pattern.compile("https?://[^\"'\\\\s]+\\.(?:mp4|m3u8)(?:\\?[^\"'\\\\s]*)?");
    private static final Pattern ESCAPED_MEDIA_URL_PATTERN = Pattern.compile("https?:\\\\/\\\\/[^\"'\\\\s]+\\.(?:mp4|m3u8)(?:\\\\/[^\"'\\\\s]*)*");
    private static final Pattern RELATIVE_MEDIA_URL_PATTERN = Pattern.compile("/[^\"'\\\\s]+\\.(?:mp4|m3u8)(?:\\?[^\"'\\\\s]*)?");
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; MultimodalParliamentExplorer/1.0)";
    private static final Path LOCAL_VIDEO_ROOT = VideoPathConfig.bundledVideoRoot();

    private final DatabaseHandler<Speech> speechDatabase;
    private final DatabaseHandler<ProtocolSession> sessionDatabase;
    private final DatabaseHandler<SpeechVideo> speechVideoDatabase;
    private final MediaAssetDownloadService mediaAssetDownloadService;

/**
 * Constructor
 */
    public AgendaVideoImportService(
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<ProtocolSession> sessionDatabase,
            DatabaseHandler<SpeechVideo> speechVideoDatabase,
            MediaAssetDownloadService mediaAssetDownloadService
    ) {
        this.speechDatabase = speechDatabase;
        this.sessionDatabase = sessionDatabase;
        this.speechVideoDatabase = speechVideoDatabase;
        this.mediaAssetDownloadService = mediaAssetDownloadService;
    }

/**
 * Method
 */
    public VideoImportResult importAgendaVideos(String protocolId, int agendaItem, String sessionUrl, boolean downloadMedia) {
        return importAgendaVideos(protocolId, agendaItem, sessionUrl, downloadMedia, 0);
    }

/**
 * Method
 */
    public VideoImportResult importAgendaVideos(String protocolId, int agendaItem, String sessionUrl, boolean downloadMedia, int maxSpeeches) {
        if (protocolId == null || protocolId.isBlank()) {
            throw new IllegalArgumentException("protocolId is required");
        }
        if (agendaItem <= 0) {
            throw new IllegalArgumentException("agendaItem must be > 0");
        }
        if (sessionUrl == null || sessionUrl.isBlank()) {
            throw new IllegalArgumentException("sessionUrl is required");
        }

        List<Speech> speeches = speechDatabase.find(
                "speeches",
                new Document("protocolId", protocolId).append("agendaItem", agendaItem),
                Speech.class
        );
        if (speeches.isEmpty()) {
            throw new IllegalArgumentException("No speeches found for protocolId=" + protocolId + " and agendaItem=" + agendaItem);
        }

        if (maxSpeeches > 0 && speeches.size() > maxSpeeches) {
            speeches = speeches.subList(0, maxSpeeches);
        }

        String sessionId = speeches.get(0).getSessionId();
        String agendaLabel = resolveAgendaLabel(sessionId, agendaItem).orElse("");
        List<SessionVideoLink> sessionLinks = loadSessionVideoLinks(sessionUrl);
        ResolvedVideoTarget target = resolveTarget(sessionLinks, agendaItem, agendaLabel);

        String sharedLocalPath = null;
        int stored = 0;
        int downloaded = 0;
        boolean canDownload = downloadMedia && target.mediaUrl() != null && !target.mediaUrl().isBlank();

        if (canDownload) {
            sharedLocalPath = mediaAssetDownloadService.downloadSharedVideo(
                    target.mediaUrl(),
                    "protocol-" + protocolId + "-top-" + agendaItem
            );
            if (sharedLocalPath != null && !sharedLocalPath.isBlank()) {
                downloaded = 1;
            }
        }

        for (Speech speech : speeches) {
            SpeechVideo video = new SpeechVideo();
            video.setId("video-" + speech.getId());
            video.setSpeechId(speech.getId());
            video.setVideoPageUrl(target.videoPageUrl());
            video.setEmbedUrl(toBundestagEmbedUrl(target.videoPageUrl()));
            video.setStreamUrl(target.mediaUrl());
            video.setSourceUrl(target.mediaUrl() != null && !target.mediaUrl().isBlank() ? target.mediaUrl() : target.videoPageUrl());
            video.setLocalPath(firstNonBlank(sharedLocalPath, resolveLocalVideoPath(target.videoPageUrl())));
            video.setDurationSeconds(0);
            speechVideoDatabase.replaceById("speech_videos", video.getId(), video);
            stored++;
        }

        return new VideoImportResult(
                protocolId,
                sessionId,
                agendaItem,
                agendaLabel,
                sessionUrl,
                target.videoPageUrl(),
                target.mediaUrl(),
                target.matchedLabel(),
                speeches.size(),
                stored,
                downloaded,
                Instant.now().toString()
        );
    }

/**
 * Method
 */
    public VideoImportResult importBestAgendaVideos(String sessionUrl, boolean downloadMedia, int maxSpeeches) {
        if (sessionUrl == null || sessionUrl.isBlank()) {
            throw new IllegalArgumentException("sessionUrl is required");
        }

        SessionHint hint = resolveSessionHint(sessionUrl);
        ProtocolSession session = findMatchingSession(hint)
                .orElseThrow(() -> new IllegalArgumentException("Could not match the mediathek session page to an imported protocol session"));

        List<SessionVideoLink> sessionLinks = loadSessionVideoLinks(sessionUrl);
        Set<Integer> availableAgendaItems = new LinkedHashSet<>();
        for (SessionVideoLink link : sessionLinks) {
            if (link.agendaItem() > 0) {
                availableAgendaItems.add(link.agendaItem());
            }
        }

        long sessionSpeechCount = speechDatabase.count("speeches", new Document("sessionId", session.getId()));
        long agendaSpeechCount = speechDatabase.count(
                "speeches",
                new Document("sessionId", session.getId()).append("agendaItem", new Document("$gt", 0))
        );

        List<Document> pipeline = List.of(
                new Document("$match", new Document("sessionId", session.getId()).append("agendaItem", new Document("$gt", 0))),
                new Document("$group", new Document("_id", "$agendaItem").append("speechCount", new Document("$sum", 1))),
                new Document("$sort", new Document("speechCount", -1).append("_id", 1))
        );
        List<Document> rows = speechDatabase.aggregate("speeches", pipeline);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException(
                    "Matched session has no speeches with agendaItem > 0: "
                            + "protocolId=" + session.getProtocolId()
                            + ", sessionId=" + session.getId()
                            + ", sessionNumber=" + session.getSessionNumber()
                            + ", sessionDate=" + session.getSessionDate()
                            + ", totalSpeeches=" + sessionSpeechCount
                            + ", agendaSpeeches=" + agendaSpeechCount
                            + ", parsedAgendaEntries=" + session.getAgenda().size()
            );
        }

        int agendaItem = 0;
        for (Document row : rows) {
            int candidate = asInt(row.get("_id"));
            if (candidate > 0 && (availableAgendaItems.isEmpty() || availableAgendaItems.contains(candidate))) {
                agendaItem = candidate;
                break;
            }
        }
        if (agendaItem <= 0) {
            throw new IllegalArgumentException(
                    "Could not determine a valid agenda item for the matched session: "
                            + "availableMediathekAgendaItems=" + availableAgendaItems
            );
        }

        return importAgendaVideos(session.getProtocolId(), agendaItem, sessionUrl, downloadMedia, maxSpeeches);
    }

/**
 * Method
 */
    public VideoImportResult importVideosForSpeech(String speechId) {
        if (speechId == null || speechId.isBlank()) {
            throw new IllegalArgumentException("speechId is required");
        }

        Speech speech = speechDatabase.findById("speeches", speechId, Speech.class)
                .orElseThrow(() -> new IllegalArgumentException("speech not found"));
        if (speech.getProtocolId() == null || speech.getProtocolId().isBlank() || speech.getAgendaItem() <= 0) {
            if (speech.getProtocolId() != null && !speech.getProtocolId().isBlank()) {
                throw new IllegalArgumentException(
                        "speech has no valid agendaItem; reimport protocol " + speech.getProtocolId() + " with the current parser first"
                );
            }
            throw new IllegalArgumentException("speech has no protocolId or valid agendaItem");
        }

        List<Speech> siblingSpeeches = speechDatabase.find(
                "speeches",
                new Document("protocolId", speech.getProtocolId()).append("agendaItem", speech.getAgendaItem()),
                Speech.class
        );
        if (siblingSpeeches.isEmpty()) {
            throw new IllegalArgumentException("no speeches found for the same protocol and agenda item");
        }

        for (Speech sibling : siblingSpeeches) {
            if (sibling == null || sibling.getId() == null || sibling.getId().isBlank() || sibling.getId().equals(speechId)) {
                continue;
            }
            SpeechVideo siblingVideo = speechVideoDatabase.findById("speech_videos", "video-" + sibling.getId(), SpeechVideo.class).orElse(null);
            if (siblingVideo == null) {
                continue;
            }
            String basePageUrl = firstNonBlank(siblingVideo.getVideoPageUrl(), siblingVideo.getSourceUrl());
            String specificClipPageUrl = resolveSpecificSpeechClipPageUrl(basePageUrl, speech);
            String effectiveVideoPageUrl = firstNonBlank(specificClipPageUrl, siblingVideo.getVideoPageUrl(), siblingVideo.getSourceUrl());
            String resolvedStreamUrl = firstNonBlank(
                    siblingVideo.getStreamUrl(),
                    resolvePlayableMediaUrl(effectiveVideoPageUrl),
                    resolvePlayableMediaUrl(basePageUrl)
            );

            SpeechVideo video = new SpeechVideo();
            video.setId("video-" + speech.getId());
            video.setSpeechId(speech.getId());
            video.setSourceUrl(firstNonBlank(resolvedStreamUrl, effectiveVideoPageUrl, siblingVideo.getSourceUrl()));
            video.setVideoPageUrl(effectiveVideoPageUrl);
            video.setEmbedUrl(toBundestagEmbedUrl(effectiveVideoPageUrl));
            video.setStreamUrl(resolvedStreamUrl);
            video.setLocalPath(firstNonBlank(resolveLocalVideoPath(effectiveVideoPageUrl), siblingVideo.getLocalPath()));
            video.setDurationSeconds(siblingVideo.getDurationSeconds());
            speechVideoDatabase.replaceById("speech_videos", video.getId(), video);

            return new VideoImportResult(
                    speech.getProtocolId(),
                    speech.getSessionId(),
                    speech.getAgendaItem(),
                    resolveAgendaLabel(speech.getSessionId(), speech.getAgendaItem()).orElse(""),
                    null,
                    effectiveVideoPageUrl,
                    video.getStreamUrl(),
                    firstNonBlank("speaker-clip-from-" + sibling.getId(), "reused-from-" + sibling.getId()),
                    1,
                    1,
                    0,
                    Instant.now().toString()
            );
        }

        throw new IllegalArgumentException(
                "no imported or downloaded clips found for protocolId=" + speech.getProtocolId()
                        + ", agendaItem=" + speech.getAgendaItem()
                        + localBundleHint()
        );
    }

    private String localBundleHint() {
        if (!Files.isDirectory(LOCAL_VIDEO_ROOT)) {
            return "; no local video bundles found under " + LOCAL_VIDEO_ROOT;
        }
        try (var paths = Files.list(LOCAL_VIDEO_ROOT)) {
            List<String> bundles = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .limit(8)
                    .collect(Collectors.toList());
            if (bundles.isEmpty()) {
                return "; no local video bundles found under " + LOCAL_VIDEO_ROOT;
            }
            return "; available local bundle(s): " + String.join(", ", bundles);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String resolveSpecificSpeechClipPageUrl(String pageUrl, Speech speech) {
        if (pageUrl == null || pageUrl.isBlank() || speech == null || speech.getSpeaker() == null) {
            return null;
        }

        String fullName = normalizeSpeakerName(speech);
        if (fullName.isBlank()) {
            return null;
        }
        List<String> speakerTokens = tokens(fullName);
        if (speakerTokens.isEmpty()) {
            return null;
        }

        Candidate best = null;
        for (SessionVideoLink link : loadSessionVideoLinks(pageUrl)) {
            if (link == null || link.url() == null || link.url().isBlank() || link.url().equals(pageUrl)) {
                continue;
            }
            String normalizedLabel = normalize(link.label());
            String normalizedContext = normalize(link.context());
            int score = 0;
            score += tokenOverlapScore(speakerTokens, tokens(normalizedLabel)) * 6;
            score += tokenOverlapScore(speakerTokens, tokens(normalizedContext)) * 4;
            if (speech.getAgendaItem() > 0 && link.agendaItem() == speech.getAgendaItem()) {
                score += 3;
            }
            String lastName = normalize(firstNonBlank(speech.getSpeaker().getLastName()));
            if (!lastName.isBlank() && (normalizedLabel.contains(lastName) || normalizedContext.contains(lastName))) {
                score += 10;
            }
            if (score > 0 && (best == null || score > best.score())) {
                best = new Candidate(link.url(), link.label(), score);
            }
        }
        return best == null ? null : best.url();
    }

    private Optional<String> resolveAgendaLabel(String sessionId, int agendaItem) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        Optional<ProtocolSession> sessionOpt = sessionDatabase.findById("sessions", sessionId, ProtocolSession.class);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        List<String> agenda = sessionOpt.get().getAgenda();
        if (agendaItem > 0 && agendaItem <= agenda.size()) {
            return Optional.ofNullable(agenda.get(agendaItem - 1));
        }
        return Optional.empty();
    }

    private ResolvedVideoTarget resolveTarget(List<SessionVideoLink> sessionLinks, int agendaItem, String agendaLabel) {
        Candidate best = null;
        for (SessionVideoLink link : sessionLinks) {
            int score = scoreCandidate(link.label(), link.context(), agendaItem, agendaLabel, link.agendaItem());
            if (best == null || score > best.score()) {
                best = new Candidate(link.url(), link.label(), score);
            }
        }
        if (best == null || best.score() <= 0) {
            throw new IllegalArgumentException("Could not match the agenda item on the provided mediathek session page");
        }

        String mediaUrl = resolvePlayableMediaUrl(best.url());
        return new ResolvedVideoTarget(best.url(), mediaUrl, best.label());
    }

    private List<SessionVideoLink> loadSessionVideoLinks(String sessionUrl) {
        try {
            org.jsoup.nodes.Document sessionDoc = Jsoup.connect(sessionUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            List<SessionVideoLink> linksOut = new ArrayList<>();
            Set<String> seen = new LinkedHashSet<>();
            Elements links = sessionDoc.select("a[href*=/mediathek/video?videoid=]");
            for (Element link : links) {
                String href = absoluteUrl(link, "href");
                if (href.isBlank() || !seen.add(href)) {
                    continue;
                }
                String label = firstNonBlank(link.text(), contextText(link));
                String context = contextText(link);
                int parsedAgendaItem = parseAgendaNumberFromText(context);
                linksOut.add(new SessionVideoLink(href, label, context, parsedAgendaItem));
            }
            return linksOut;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not resolve agenda videos from the mediathek session page: " + ex.getMessage(), ex);
        }
    }

    private SessionHint resolveSessionHint(String sessionUrl) {
        try {
            org.jsoup.nodes.Document sessionDoc = Jsoup.connect(sessionUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            String rawTitle = firstNonBlank(sessionDoc.title(), "");
            String rawText = firstNonBlank(sessionDoc.text(), "");
            String rawCombined = rawTitle + " " + rawText;
            Integer sessionNumber = null;
            LocalDate sessionDate = null;

            Matcher sessionMatcher = Pattern.compile("(\\d{1,3})\\s*\\.\\s*Sitzung", Pattern.CASE_INSENSITIVE).matcher(rawCombined);
            if (sessionMatcher.find()) {
                sessionNumber = Integer.parseInt(sessionMatcher.group(1));
            }

            Matcher dateMatcher = Pattern.compile("(\\d{2})\\.(\\d{2})\\.(\\d{4})").matcher(rawCombined);
            if (dateMatcher.find()) {
                sessionDate = LocalDate.of(
                        Integer.parseInt(dateMatcher.group(3)),
                        Integer.parseInt(dateMatcher.group(2)),
                        Integer.parseInt(dateMatcher.group(1))
                );
            }

            return new SessionHint(sessionNumber, sessionDate);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not inspect the mediathek session page: " + ex.getMessage(), ex);
        }
    }

    private Optional<ProtocolSession> findMatchingSession(SessionHint hint) {
        if (hint.sessionNumber() == null) {
            return Optional.empty();
        }

        List<ProtocolSession> sessions = sessionDatabase.find("sessions", new Document("sessionNumber", hint.sessionNumber()), ProtocolSession.class);
        if (sessions.isEmpty()) {
            return Optional.empty();
        }

        List<ProtocolSession> withSpeeches = sessions.stream()
                .filter(session -> speechDatabase.count("speeches", new Document("sessionId", session.getId())) > 0)
                .toList();
        if (!withSpeeches.isEmpty()) {
            sessions = withSpeeches;
        }

        if (hint.sessionDate() != null) {
            for (ProtocolSession session : sessions) {
                if (hint.sessionDate().equals(session.getSessionDate())) {
                    return Optional.of(session);
                }
            }
        }

        return sessions.stream()
                .sorted(
                        Comparator.<ProtocolSession>comparingLong(session ->
                                        speechDatabase.count("speeches", new Document("sessionId", session.getId())))
                                .reversed()
                                .thenComparing(Comparator.comparingInt(ProtocolSession::getLegislativePeriod).reversed())
                )
                .findFirst();
    }

    private String resolvePlayableMediaUrl(String videoPageUrl) {
        try {
            org.jsoup.nodes.Document doc = Jsoup.connect(videoPageUrl)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();

            for (String css : List.of(
                    "meta[property=og:video]",
                    "meta[property=og:video:url]",
                    "meta[name=twitter:player:stream]",
                    "video source[src]",
                    "source[src]",
                    "a[href$=.mp4]",
                    "a[href*=.mp4?]",
                    "a[href$=.m3u8]",
                    "a[href*=.m3u8?]"
            )) {
                for (Element element : doc.select(css)) {
                    String candidate = absoluteUrl(element, element.hasAttr("content") ? "content" : "src");
                    if (candidate.isBlank()) {
                        candidate = absoluteUrl(element, "href");
                    }
                    if (isPlayableMediaUrl(candidate)) {
                        return candidate;
                    }
                }
            }

            Matcher matcher = MEDIA_URL_PATTERN.matcher(doc.outerHtml());
            if (matcher.find()) {
                return matcher.group();
            }

            String decodedHtml = decodeEscapedHtml(doc.outerHtml());
            matcher = MEDIA_URL_PATTERN.matcher(decodedHtml);
            if (matcher.find()) {
                return matcher.group();
            }

            Matcher escapedMatcher = ESCAPED_MEDIA_URL_PATTERN.matcher(doc.outerHtml());
            if (escapedMatcher.find()) {
                return escapedMatcher.group().replace("\\/", "/");
            }

            Matcher relativeMatcher = RELATIVE_MEDIA_URL_PATTERN.matcher(decodedHtml);
            while (relativeMatcher.find()) {
                String candidate = relativeMatcher.group();
                if (isPlayableMediaUrl(candidate)) {
                    return toAbsoluteBundestagUrl(candidate);
                }
            }

            for (Pattern jsonPattern : List.of(
                    Pattern.compile("\"(?:src|file|url|streamUrl|videoUrl)\"\\s*:\\s*\"([^\"]+\\.(?:mp4|m3u8)[^\"]*)\""),
                    Pattern.compile("'(?:src|file|url|streamUrl|videoUrl)'\\s*:\\s*'([^']+\\.(?:mp4|m3u8)[^']*)'"),
                    Pattern.compile("(?:src|file|url|streamUrl|videoUrl)\\s*[:=]\\s*\"([^\"]+\\.(?:mp4|m3u8)[^\"]*)\""),
                    Pattern.compile("(?:src|file|url|streamUrl|videoUrl)\\s*[:=]\\s*'([^']+\\.(?:mp4|m3u8)[^']*)'")
            )) {
                Matcher jsonMatcher = jsonPattern.matcher(decodedHtml);
                if (jsonMatcher.find()) {
                    return toAbsoluteBundestagUrl(jsonMatcher.group(1));
                }
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int scoreCandidate(String label, String context, int agendaItem, String agendaLabel, int parsedAgendaItem) {
        String normalizedLabel = normalize(label);
        String normalizedContext = normalize(context);
        String normalizedAgenda = normalize(agendaLabel);

        int score = 0;
        if (parsedAgendaItem > 0 && parsedAgendaItem == agendaItem) {
            score += 20;
        }
        if (containsAgendaNumber(normalizedLabel, agendaItem) || containsAgendaNumber(normalizedContext, agendaItem)) {
            score += 8;
        }

        if (!normalizedAgenda.isBlank()) {
            score += tokenOverlapScore(tokens(normalizedAgenda), tokens(normalizedLabel)) * 3;
            score += tokenOverlapScore(tokens(normalizedAgenda), tokens(normalizedContext)) * 2;
        }

        if (normalizedContext.contains("videos der tagesordnungspunkte")) {
            score += 2;
        }
        if (normalizedLabel.isBlank()) {
            score -= 2;
        }
        return score;
    }

    private int parseAgendaNumberFromText(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Matcher topMatcher = Pattern.compile("\\b(?:TOP|ZP)\\s*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (topMatcher.find()) {
            return Integer.parseInt(topMatcher.group(1));
        }

        Matcher dayItemMatcher = Pattern.compile("\\bTagesordnungspunkt\\s*(\\d{1,3})\\b", Pattern.CASE_INSENSITIVE).matcher(text);
        if (dayItemMatcher.find()) {
            return Integer.parseInt(dayItemMatcher.group(1));
        }

        Matcher direct = Pattern.compile("\\b(\\d{1,3})\\b").matcher(text);
        while (direct.find()) {
            int value = Integer.parseInt(direct.group(1));
            if (value > 0 && value < 400) {
                return value;
            }
        }
        return 0;
    }

    private boolean containsAgendaNumber(String text, int agendaItem) {
        String top = "top " + agendaItem;
        String compactTop = "top" + agendaItem;
        String dayItem = "tagesordnungspunkt " + agendaItem;
        return text.contains(top) || text.contains(compactTop) || text.contains(dayItem);
    }

    private int tokenOverlapScore(Collection<String> left, Collection<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        Set<String> rightSet = new LinkedHashSet<>(right);
        int score = 0;
        for (String token : left) {
            if (rightSet.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> tokens(String text) {
        List<String> values = new ArrayList<>();
        for (String token : text.split("\\s+")) {
            if (token.length() >= 3) {
                values.add(token);
            }
        }
        return values;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9äöüß]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String normalizeSpeakerName(Speech speech) {
        if (speech == null || speech.getSpeaker() == null) {
            return "";
        }
        return normalize(
                (firstNonBlank(speech.getSpeaker().getTitle(), "") + " "
                        + firstNonBlank(speech.getSpeaker().getFirstName(), "") + " "
                        + firstNonBlank(speech.getSpeaker().getLastName(), ""))
                        .trim()
        );
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String contextText(Element element) {
        Element container = element.closest("tr,li,article,section,div");
        if (container != null) {
            return container.text();
        }
        return element.parent() != null ? element.parent().text() : element.text();
    }

    private boolean isPlayableMediaUrl(String value) {
        String lower = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return lower.contains(".mp4") || lower.contains(".m3u8");
    }

    private String decodeEscapedHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
                .replace("\\/", "/")
                .replace("\\u002F", "/")
                .replace("\\u003A", ":")
                .replace("&quot;", "\"");
    }

    private String toAbsoluteBundestagUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return value;
        }
        if (value.startsWith("//")) {
            return "https:" + value;
        }
        if (value.startsWith("/")) {
            return "https://www.bundestag.de" + value;
        }
        return value;
    }

    private String absoluteUrl(Element element, String attr) {
        String value = element.attr(attr);
        if (value == null || value.isBlank()) {
            return "";
        }
        String absolute = element.absUrl(attr);
        if (absolute != null && !absolute.isBlank()) {
            return absolute;
        }
        try {
            return URI.create(value).toString();
        } catch (Exception ignored) {
            return value;
        }
    }

    private String toBundestagEmbedUrl(String videoPageUrl) {
        if (videoPageUrl == null || videoPageUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(videoPageUrl);
            String query = uri.getQuery();
            if (query == null || !query.contains("videoid=")) {
                return null;
            }
            String videoId = null;
            for (String entry : query.split("&")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2 && "videoid".equals(parts[0]) && !parts[1].isBlank()) {
                    videoId = parts[1];
                    break;
                }
            }
            if (videoId == null) {
                return null;
            }
            return "https://www.bundestag.de/mediathekoverlay?videoid=" + videoId + "&mod=mediathek";
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveLocalVideoPath(String videoPageUrl) {
        String videoId = extractBundestagVideoId(videoPageUrl);
        if (videoId == null || videoId.isBlank() || !Files.isDirectory(LOCAL_VIDEO_ROOT)) {
            return null;
        }
        try (var paths = Files.walk(LOCAL_VIDEO_ROOT, 6)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> VideoPathConfig.fileNameMatchesVideoId(path.getFileName().toString(), videoId))
                    .map(path -> path.toAbsolutePath().toString())
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractBundestagVideoId(String videoPageUrl) {
        if (videoPageUrl == null || videoPageUrl.isBlank() || !videoPageUrl.contains("videoid=")) {
            return null;
        }
        int start = videoPageUrl.indexOf("videoid=");
        if (start < 0) {
            return null;
        }
        String rest = videoPageUrl.substring(start + "videoid=".length());
        int end = rest.indexOf('&');
        String videoId = end >= 0 ? rest.substring(0, end) : rest;
        return videoId.isBlank() ? null : videoId;
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private record Candidate(String url, String label, int score) {
    }

    private record ResolvedVideoTarget(String videoPageUrl, String mediaUrl, String matchedLabel) {
    }

    private record SessionVideoLink(String url, String label, String context, int agendaItem) {
    }

    private record SessionHint(Integer sessionNumber, LocalDate sessionDate) {
    }

/**
 * VideoImportResult service
 */
    public record VideoImportResult(
            String protocolId,
            String sessionId,
            int agendaItem,
            String agendaLabel,
            String sessionUrl,
            String matchedVideoPageUrl,
            String matchedMediaUrl,
            String matchedLabel,
            int speechesMatched,
            int videosStored,
            int downloadedFiles,
            String finishedAt
    ) {
    }
}
