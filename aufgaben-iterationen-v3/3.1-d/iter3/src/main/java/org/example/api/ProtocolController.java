package org.example.api;

import org.example.db.DatabaseHandler;
import org.example.model.Deputy;
import org.example.model.ProtocolDocument;
import org.example.model.ProtocolSession;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.example.service.DeputyImageEnrichmentService;
import org.example.service.NlpProcessingService;
import org.example.service.nlp.TextNormalizationUtil;
import org.example.util.VideoPathConfig;
import io.javalin.Javalin;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolController controller
 */
public final class ProtocolController {
    private static final int MAX_QUERY_LIMIT = 1000;
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static final Path LOCAL_VIDEO_ROOT = VideoPathConfig.bundledVideoRoot();

    private ProtocolController() {
    }

/**
 * Method
 */
    public static void register(
            Javalin app,
            DatabaseHandler<ProtocolDocument> protocolDatabase,
            DatabaseHandler<ProtocolSession> sessionDatabase,
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<Deputy> deputyDatabase,
            DatabaseHandler<SpeechVideo> speechVideoDatabase,
            DeputyImageEnrichmentService deputyImageEnrichmentService,
            NlpProcessingService nlpProcessingService
    ) {
        app.get("/api/protocols", ctx -> {
            try {
                int limit = parseInt(ctx.queryParam("limit"), 50, "limit");
                boolean includeRaw = parseBoolean(ctx.queryParam("includeRaw"), false, "includeRaw");
                List<ProtocolDocument> protocols = protocolDatabase.findLimited("protocols", new Document(), ProtocolDocument.class, limit);
                if (includeRaw) {
                    ctx.json(protocols);
                    return;
                }
                ctx.json(protocols.stream().map(ProtocolSummary::from).toList());
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/protocols/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<ProtocolDocument> protocol = protocolDatabase.findById("protocols", id, ProtocolDocument.class);
            if (protocol.isEmpty()) {
                ctx.status(404).json(new Document("error", "protocol not found"));
                return;
            }
            boolean includeRaw;
            try {
                includeRaw = parseBoolean(ctx.queryParam("includeRaw"), false, "includeRaw");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }
            if (includeRaw) {
                ctx.json(protocol.get());
                return;
            }
            ctx.json(ProtocolSummary.from(protocol.get()));
        });

        app.get("/api/sessions", ctx -> {
            Document filter = new Document();
            String protocolId = ctx.queryParam("protocolId");
            if (protocolId != null && !protocolId.isBlank()) {
                filter.append("protocolId", protocolId);
            }
            ctx.json(sessionDatabase.find("sessions", filter, ProtocolSession.class));
        });

        app.get("/api/speeches", ctx -> {
            try {
                String agendaItem = ctx.queryParam("agendaItem");
                Integer agendaItemValue = null;
                if (agendaItem != null && !agendaItem.isBlank()) {
                    agendaItemValue = parseInt(agendaItem, 0, "agendaItem");
                }
                Document filter = SpeechQueryFilterBuilder.build(
                        ctx.queryParam("protocolId"),
                        ctx.queryParam("protocolIds"),
                        ctx.queryParam("sessionId"),
                        ctx.queryParam("speakerId"),
                        ctx.queryParam("faction"),
                        ctx.queryParam("topic"),
                        agendaItemValue,
                        ctx.queryParam("matchMode")
                );
                Instant from = parseInstantNullable(ctx.queryParam("from"), "from");
                Instant to = parseInstantNullable(ctx.queryParam("to"), "to");

                int limit = parseInt(ctx.queryParam("limit"), 100, "limit");
                List<Speech> speeches = speechDatabase.findLimited("speeches", filter, Speech.class, limit);
                if (from != null || to != null) {
                    speeches = speeches.stream().filter(speech -> {
                        Instant startedAt = speech.getStartedAt();
                        if (startedAt == null) {
                            return false;
                        }
                        if (from != null && startedAt.isBefore(from)) {
                            return false;
                        }
                        if (to != null && startedAt.isAfter(to)) {
                            return false;
                        }
                        return true;
                    }).toList();
                }
                ctx.json(speeches);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/speeches/search", ctx -> {
            String q = ctx.queryParam("q");
            int limit;
            try {
                limit = parseInt(ctx.queryParam("limit"), 100, "limit");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }
            if (q == null || q.isBlank()) {
                ctx.status(400).json(new Document("error", "q is required"));
                return;
            }

            Document regex = new Document("$regex", q).append("$options", "i");
            Document filter = new Document("text", regex);
            List<Speech> speeches = speechDatabase.findLimited("speeches", filter, Speech.class, limit);
            ctx.json(speeches);
        });

        app.get("/api/speeches/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<Speech> speech = speechDatabase.findById("speeches", id, Speech.class);
            if (speech.isEmpty()) {
                ctx.status(404).json(new Document("error", "speech not found"));
                return;
            }
            Speech current = speech.get();
            String raw = current.getText();
            String fixed = TextNormalizationUtil.sanitizeSpeechText(raw);
            if (raw == null ? fixed != null : !raw.equals(fixed)) {
                current.setText(fixed);
                if (current.getId() != null && !current.getId().isBlank()) {
                    speechDatabase.replaceById("speeches", current.getId(), current);
                }
            }
            ctx.json(current);
        });

        app.get("/api/speeches/{id}/detail", ctx -> {
            String id = ctx.pathParam("id");
            boolean ensureNlp;
            try {
                ensureNlp = parseBoolean(ctx.queryParam("ensureNlp"), true, "ensureNlp");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }

            Optional<Speech> initialSpeechOpt = speechDatabase.findById("speeches", id, Speech.class);
            if (initialSpeechOpt.isEmpty()) {
                ctx.status(404).json(new Document("error", "speech not found"));
                return;
            }
            Speech initialSpeech = initialSpeechOpt.get();
            boolean forceNlp = containsLikelyMojibake(initialSpeech.getText());
            String rawText = initialSpeech.getText();
            String fixedText = TextNormalizationUtil.sanitizeSpeechText(rawText);
            if (rawText == null ? fixedText != null : !rawText.equals(fixedText)) {
                initialSpeech.setText(fixedText);
                if (initialSpeech.getId() != null && !initialSpeech.getId().isBlank()) {
                    speechDatabase.replaceById("speeches", initialSpeech.getId(), initialSpeech);
                }
                forceNlp = true;
            }

            if (ensureNlp) {
                try {
                    nlpProcessingService.runSingleSpeech(id, forceNlp);
                } catch (Exception ex) {
                    ctx.header("X-NLP-Warning", "run-failed");
                }
            }

            Speech speech = speechDatabase.findById("speeches", id, Speech.class).orElse(initialSpeech);
            Deputy speaker = null;
            if (speech.getSpeaker() != null && speech.getSpeaker().getId() != null && !speech.getSpeaker().getId().isBlank()) {
                speaker = deputyDatabase.findById("deputies", speech.getSpeaker().getId(), Deputy.class).orElse(speech.getSpeaker());
            }

            Document videoFilter = new Document("speechId", speech.getId());
            SpeechVideo video = speechVideoDatabase.find("speech_videos", videoFilter, SpeechVideo.class)
                    .stream()
                    .findFirst()
                    .map(ProtocolController::normalizeSpeechVideo)
                    .orElseGet(() -> findSiblingAgendaVideo(speech, speechDatabase, speechVideoDatabase));
            ClipWindow clipWindow = (video != null && video.getLocalPath() != null && !video.getLocalPath().isBlank())
                    ? null
                    : resolveClipWindow(speech, speechDatabase);
            ctx.json(new SpeechDetailResponse(
                    speech,
                    speaker,
                    video,
                    clipWindow == null ? null : clipWindow.startSeconds(),
                    clipWindow == null ? null : clipWindow.endSeconds()
            ));
        });

        app.get("/api/deputies", ctx -> {
            try {
                int limit = parseInt(ctx.queryParam("limit"), 200, "limit");
                List<Deputy> deputies = deputyDatabase.findLimited("deputies", new Document(), Deputy.class, limit);
                ctx.json(deputies);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/deputies/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<Deputy> deputy = deputyDatabase.findById("deputies", id, Deputy.class);
            if (deputy.isEmpty()) {
                ctx.status(404).json(new Document("error", "deputy not found"));
                return;
            }
            ctx.json(deputy.get());
        });

        app.get("/api/deputies/{id}/image", ctx -> {
            String id = ctx.pathParam("id");
            Optional<Deputy> deputyOpt = deputyDatabase.findById("deputies", id, Deputy.class);
            if (deputyOpt.isEmpty()) {
                ctx.status(404).json(new Document("error", "deputy not found"));
                return;
            }
            Deputy deputy = deputyOpt.get();
            if (deputy.getImages() != null) {
                deputy.getImages().removeIf(image -> {
                    String sourceUrl = image == null ? null : image.getSourceUrl();
                    if (sourceUrl == null) {
                        return false;
                    }
                    String lower = sourceUrl.toLowerCase();
                    return lower.contains("matomo")
                            || lower.contains("statistik.bundestag.de")
                            || lower.contains("/piwik")
                            || lower.contains("tracking")
                            || lower.contains("/includes/images/layout/")
                            || lower.contains("dummy_16_9");
                });
            }
            if (deputy.getImages() == null || deputy.getImages().isEmpty()) {
                deputyImageEnrichmentService.enrichWithProfileImage(deputy);
                if (deputy.getImages() != null && !deputy.getImages().isEmpty()) {
                    deputyDatabase.replaceById("deputies", deputy.getId(), deputy);
                }
            }
            if (deputy.getImages() == null || deputy.getImages().isEmpty()) {
                ctx.status(404).json(new Document("error", "deputy image not found"));
                return;
            }
            var image = deputy.getImages().stream()
                    .filter(img -> img != null
                            && ((img.getLocalPath() != null && !img.getLocalPath().isBlank())
                            || (img.getSourceUrl() != null && !img.getSourceUrl().isBlank())))
                    .findFirst()
                    .orElse(null);
            if (image == null) {
                ctx.status(404).json(new Document("error", "deputy image not found"));
                return;
            }
            if (image.getLocalPath() != null && !image.getLocalPath().isBlank()) {
                Path path = Path.of(image.getLocalPath());
                if (Files.isRegularFile(path)) {
                    String mimeType = firstNonBlank(image.getMimeType(), safeProbeContentType(path), "image/jpeg");
                    ctx.contentType(mimeType);
                    ctx.result(Files.newInputStream(path));
                    return;
                }
            }
            if (image.getSourceUrl() != null && !image.getSourceUrl().isBlank()) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(image.getSourceUrl()))
                            .header("User-Agent", "Mozilla/5.0 (compatible; MultimodalParliamentExplorer/1.0)")
                            .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                            .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.8")
                            .header("Referer", "https://bilddatenbank.bundestag.de/")
                            .GET()
                            .build();
                    HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        String mimeType = firstNonBlank(
                                image.getMimeType(),
                                response.headers().firstValue("Content-Type").orElse(null),
                                "image/jpeg"
                        );
                        if (!mimeType.toLowerCase().startsWith("image/")) {
                            ctx.status(404).json(new Document("error", "deputy image not found"));
                            return;
                        }
                        ctx.contentType(mimeType);
                        ctx.result(response.body());
                        return;
                    }
                } catch (Exception ignored) {
                }
            }
            ctx.status(404).json(new Document("error", "deputy image not found"));
        });

        app.get("/api/deputies/{id}/image/debug", ctx -> {
            String id = ctx.pathParam("id");
            Optional<Deputy> deputyOpt = deputyDatabase.findById("deputies", id, Deputy.class);
            if (deputyOpt.isEmpty()) {
                ctx.status(404).json(new Document("error", "deputy not found"));
                return;
            }
            ctx.json(deputyImageEnrichmentService.debugProfileImageSearch(deputyOpt.get()));
        });

        app.get("/api/videos", ctx -> {
            try {
                Document filter = new Document();
                int limit = parseInt(ctx.queryParam("limit"), 200, "limit");
                String speechId = ctx.queryParam("speechId");
                String speechIds = ctx.queryParam("speechIds");
                if (speechIds != null && !speechIds.isBlank()) {
                    List<String> ids = Arrays.stream(speechIds.split(","))
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .distinct()
                            .toList();
                    if (!ids.isEmpty()) {
                        filter.append("speechId", new Document("$in", ids));
                    }
                } else {
                    appendFilter(filter, "speechId", speechId);
                }
                List<SpeechVideo> videos = speechVideoDatabase.findLimited("speech_videos", filter, SpeechVideo.class, limit)
                        .stream()
                        .map(ProtocolController::normalizeSpeechVideo)
                        .toList();
                ctx.json(videos);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/videos/{id}", ctx -> {
            String id = ctx.pathParam("id");
            Optional<SpeechVideo> video = speechVideoDatabase.findById("speech_videos", id, SpeechVideo.class);
            if (video.isEmpty()) {
                ctx.status(404).json(new Document("error", "video not found"));
                return;
            }
            ctx.json(normalizeSpeechVideo(video.get()));
        });

        app.get("/api/videos/{id}/file", ctx -> {
            String id = ctx.pathParam("id");
            Optional<SpeechVideo> videoOpt = speechVideoDatabase.findById("speech_videos", id, SpeechVideo.class);
            if (videoOpt.isEmpty()) {
                ctx.status(404).contentType("text/plain").result("video not found");
                return;
            }

            SpeechVideo video = normalizeSpeechVideo(videoOpt.get());
            Path path = resolveLocalVideoFile(video);
            if (path == null || !Files.isRegularFile(path)) {
                ctx.status(404).contentType("text/plain").result("local video file not found");
                return;
            }

            long fileLength = Files.size(path);
            String mimeType = Files.probeContentType(path);
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "video/mp4";
            }
            ctx.res().setHeader("Accept-Ranges", "bytes");
            ctx.contentType(mimeType);

            String range = ctx.header("Range");
            if (range == null || !range.startsWith("bytes=")) {
                ctx.status(200);
                ctx.res().setHeader("Content-Length", String.valueOf(fileLength));
                try (var in = Files.newInputStream(path); var out = ctx.res().getOutputStream()) {
                    in.transferTo(out);
                }
                return;
            }

            long start = 0;
            long end = fileLength - 1;
            String value = range.substring("bytes=".length()).trim();
            int dash = value.indexOf('-');
            if (dash >= 0) {
                String startText = value.substring(0, dash).trim();
                String endText = value.substring(dash + 1).trim();
                if (!startText.isBlank()) {
                    start = Long.parseLong(startText);
                }
                if (!endText.isBlank()) {
                    end = Long.parseLong(endText);
                }
            }

            start = Math.max(0, start);
            end = Math.min(end, fileLength - 1);
            if (start > end || start >= fileLength) {
                ctx.status(416);
                ctx.res().setHeader("Content-Range", "bytes */" + fileLength);
                return;
            }

            long contentLength = end - start + 1;
            ctx.status(206);
            ctx.res().setHeader("Content-Length", String.valueOf(contentLength));
            ctx.res().setHeader("Content-Range", "bytes " + start + "-" + end + "/" + fileLength);
            try (var raf = new java.io.RandomAccessFile(path.toFile(), "r");
                 var out = ctx.res().getOutputStream()) {
                raf.seek(start);
                byte[] buffer = new byte[8192];
                long remaining = contentLength;
                while (remaining > 0) {
                    int read = raf.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (read < 0) {
                        break;
                    }
                    out.write(buffer, 0, read);
                    remaining -= read;
                }
            }
        });

        app.get("/api/videos/{id}/embed", ctx -> {
            String id = ctx.pathParam("id");
            Optional<SpeechVideo> videoOpt = speechVideoDatabase.findById("speech_videos", id, SpeechVideo.class);
            if (videoOpt.isEmpty()) {
                ctx.status(404).contentType("text/plain").result("video not found");
                return;
            }

            SpeechVideo video = normalizeSpeechVideo(videoOpt.get());
            String remoteUrl = firstNonBlank(video.getVideoPageUrl(), video.getEmbedUrl(), video.getSourceUrl());
            if (remoteUrl == null || remoteUrl.isBlank()) {
                ctx.status(404).contentType("text/plain").result("no embeddable video URL available");
                return;
            }

            if (isDirectMediaUrl(remoteUrl)) {
                ctx.redirect(remoteUrl);
                return;
            }

            try {
                String html = fetchRemoteEmbedHtml(remoteUrl);
                ctx.contentType("text/html; charset=utf-8").result(html);
            } catch (Exception ex) {
                ctx.status(502).contentType("text/plain").result("could not load remote video embed: " + ex.getMessage());
            }
        });

        app.get("/api/videos/{id}/player", ctx -> {
            String id = ctx.pathParam("id");
            Optional<SpeechVideo> videoOpt = speechVideoDatabase.findById("speech_videos", id, SpeechVideo.class);
            if (videoOpt.isEmpty()) {
                ctx.status(404).contentType("text/plain").result("video not found");
                return;
            }

            SpeechVideo video = normalizeSpeechVideo(videoOpt.get());
            String videoPageUrl = firstNonBlank(video.getVideoPageUrl(), video.getSourceUrl());
            String videoId = extractBundestagVideoId(videoPageUrl);
            if (videoId == null || videoId.isBlank()) {
                ctx.status(404).contentType("text/plain").result("no Bundestag clip videoid available");
                return;
            }

            ctx.contentType("text/html; charset=utf-8").result(buildBundestagPlayerPage(videoId));
        });

        app.get("/api/video-candidates", ctx -> {
            try {
                int limit = parseInt(ctx.queryParam("limit"), 10, "limit");
                int minSpeeches = parseInt(ctx.queryParam("minSpeeches"), 3, "minSpeeches");

                List<Document> pipeline = List.of(
                        new Document("$match", new Document("agendaItem", new Document("$gt", 0))),
                        new Document("$group", new Document("_id", new Document("protocolId", "$protocolId")
                                .append("sessionId", "$sessionId")
                                .append("agendaItem", "$agendaItem"))
                                .append("speechCount", new Document("$sum", 1))
                                .append("speakerIds", new Document("$addToSet", "$speaker.id"))),
                        new Document("$project", new Document("_id", 1)
                                .append("speechCount", 1)
                                .append("speakerCount", new Document("$size", new Document("$filter", new Document("input", "$speakerIds")
                                        .append("as", "speakerId")
                                        .append("cond", new Document("$and", Arrays.asList(
                                                new Document("$ne", Arrays.asList("$$speakerId", null)),
                                                new Document("$ne", Arrays.asList("$$speakerId", ""))
                                        ))))))),
                        new Document("$match", new Document("speechCount", new Document("$gte", minSpeeches))),
                        new Document("$sort", new Document("speechCount", -1)
                                .append("speakerCount", -1)
                                .append("_id.protocolId", 1)
                                .append("_id.agendaItem", 1)),
                        new Document("$limit", limit)
                );

                List<Document> rows = speechDatabase.aggregate("speeches", pipeline);
                List<AgendaVideoCandidate> candidates = new ArrayList<>();
                for (Document row : rows) {
                    Document id = row.get("_id", Document.class);
                    if (id == null) {
                        continue;
                    }

                    String protocolId = id.getString("protocolId");
                    String sessionId = id.getString("sessionId");
                    int agendaItem = asInt(id.get("agendaItem"));
                    if (protocolId == null || protocolId.isBlank() || sessionId == null || sessionId.isBlank() || agendaItem <= 0) {
                        continue;
                    }

                    Optional<ProtocolSession> sessionOpt = sessionDatabase.findById("sessions", sessionId, ProtocolSession.class);
                    int legislativePeriod = 0;
                    int sessionNumber = 0;
                    String agendaLabel = "";
                    if (sessionOpt.isPresent()) {
                        ProtocolSession session = sessionOpt.get();
                        legislativePeriod = session.getLegislativePeriod();
                        sessionNumber = session.getSessionNumber();
                        if (agendaItem <= session.getAgenda().size()) {
                            agendaLabel = session.getAgenda().get(agendaItem - 1);
                        }
                    }

                    candidates.add(new AgendaVideoCandidate(
                            protocolId,
                            sessionId,
                            legislativePeriod,
                            sessionNumber,
                            agendaItem,
                            agendaLabel,
                            row.getLong("speechCount") == null ? 0L : row.getLong("speechCount"),
                            asInt(row.get("speakerCount")),
                            "/speeches?protocolId=" + protocolId + "&agendaItem=" + agendaItem
                    ));
                }
                ctx.json(candidates);
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
            }
        });

        app.get("/api/topics", ctx -> {
            int limit;
            try {
                limit = parseInt(ctx.queryParam("limit"), 100, "limit");
            } catch (IllegalArgumentException ex) {
                ctx.status(400).json(new Document("error", ex.getMessage()));
                return;
            }

            List<Document> pipeline = List.of(
                    new Document("$project", new Document("topics", new Document("$ifNull", List.of("$nlp.topics", "$topics")))),
                    new Document("$unwind", "$topics"),
                    new Document("$project", new Document("label", new Document("$ifNull", List.of("$topics.label", "$topics.topic")))),
                    new Document("$match", new Document("label", new Document("$ne", null))),
                    new Document("$group", new Document("_id", "$label").append("count", new Document("$sum", 1))),
                    new Document("$sort", new Document("count", -1)),
                    new Document("$limit", limit)
            );
            ctx.json(speechDatabase.aggregate("speeches", pipeline));
        });

        app.get("/api/stats", ctx -> ctx.json(new Document()
                .append("protocols", protocolDatabase.count("protocols", new Document()))
                .append("sessions", sessionDatabase.count("sessions", new Document()))
                .append("speeches", speechDatabase.count("speeches", new Document()))
                .append("deputies", deputyDatabase.count("deputies", new Document()))
                .append("videos", speechVideoDatabase.count("speech_videos", new Document()))));
    }

    private static void appendFilter(Document filter, String field, String value) {
        if (value != null && !value.isBlank()) {
            filter.append(field, value);
        }
    }

    private static int parseInt(String input, int fallback, String name) {
        try {
            if (input == null || input.isBlank()) {
                return fallback;
            }
            int parsed = Integer.parseInt(input);
            if (parsed < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
            if ("limit".equals(name) && parsed > MAX_QUERY_LIMIT) {
                throw new IllegalArgumentException("limit must be <= " + MAX_QUERY_LIMIT);
            }
            return parsed;
        } catch (NumberFormatException ignored) {
            throw new IllegalArgumentException(name + " must be an integer");
        }
    }

    private static boolean parseBoolean(String input, boolean fallback, String name) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(input)) {
            return true;
        }
        if ("false".equalsIgnoreCase(input)) {
            return false;
        }
        throw new IllegalArgumentException(name + " must be true or false");
    }

    private static Instant parseInstantNullable(String input, String name) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(input);
        } catch (Exception ignored) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 instant");
        }
    }

    private static int asInt(Object value) {
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

    private static SpeechVideo normalizeSpeechVideo(SpeechVideo video) {
        if (video == null) {
            return null;
        }
        Path localFile = resolveLocalVideoFile(video);
        if (localFile != null) {
            video.setLocalPath(localFile.toAbsolutePath().toString());
            video.setStreamUrl("/api/videos/" + video.getId() + "/file");
        }
        if ((video.getVideoPageUrl() == null || video.getVideoPageUrl().isBlank())
                && video.getSourceUrl() != null
                && video.getSourceUrl().contains("bundestag.de/mediathek/video")) {
            video.setVideoPageUrl(video.getSourceUrl());
        }
        if ((video.getEmbedUrl() == null || video.getEmbedUrl().isBlank()) && video.getVideoPageUrl() != null) {
            String embedUrl = deriveBundestagEmbedUrl(video.getVideoPageUrl());
            if (embedUrl != null) {
                video.setEmbedUrl(embedUrl);
            }
        }
        if ((video.getStreamUrl() == null || video.getStreamUrl().isBlank())
                && video.getSourceUrl() != null
                && (video.getSourceUrl().contains(".mp4") || video.getSourceUrl().contains(".m3u8"))) {
            video.setStreamUrl(video.getSourceUrl());
        }
        return video;
    }

    private static String safeProbeContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean containsLikelyMojibake(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return text.contains("Ã")
                || text.contains("Â")
                || text.contains("â€")
                || text.contains("â€“")
                || text.contains("â€œ")
                || text.contains("â€ž")
                || text.contains("�");
    }

    private static Path resolveLocalVideoFile(SpeechVideo video) {
        if (video == null) {
            return null;
        }
        if (video.getLocalPath() != null && !video.getLocalPath().isBlank()) {
            Path path = Path.of(video.getLocalPath());
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        String videoId = extractBundestagVideoId(firstNonBlank(video.getVideoPageUrl(), video.getSourceUrl()));
        if (videoId == null || videoId.isBlank() || !Files.isDirectory(LOCAL_VIDEO_ROOT)) {
            return null;
        }
        try (var paths = Files.walk(LOCAL_VIDEO_ROOT, 6)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> VideoPathConfig.fileNameMatchesVideoId(path.getFileName().toString(), videoId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static SpeechVideo findSiblingAgendaVideo(
            Speech speech,
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<SpeechVideo> speechVideoDatabase
    ) {
        if (speech == null || speech.getProtocolId() == null || speech.getProtocolId().isBlank() || speech.getAgendaItem() <= 0) {
            return null;
        }

        List<Speech> siblingSpeeches = speechDatabase.findLimited(
                "speeches",
                new Document("protocolId", speech.getProtocolId()).append("agendaItem", speech.getAgendaItem()),
                Speech.class,
                200
        );
        for (Speech sibling : siblingSpeeches) {
            if (sibling == null || sibling.getId() == null || sibling.getId().isBlank() || sibling.getId().equals(speech.getId())) {
                continue;
            }
            SpeechVideo siblingVideo = speechVideoDatabase.findById("speech_videos", "video-" + sibling.getId(), SpeechVideo.class)
                    .map(ProtocolController::normalizeSpeechVideo)
                    .orElse(null);
            if (siblingVideo != null) {
                return siblingVideo;
            }
        }
        return null;
    }

    private static ClipWindow resolveClipWindow(Speech speech, DatabaseHandler<Speech> speechDatabase) {
        if (speech == null
                || speech.getProtocolId() == null
                || speech.getProtocolId().isBlank()
                || speech.getAgendaItem() <= 0
                || speech.getStartedAt() == null) {
            return null;
        }

        List<Speech> agendaSpeeches = speechDatabase.find(
                "speeches",
                new Document("protocolId", speech.getProtocolId()).append("agendaItem", speech.getAgendaItem()),
                Speech.class
        ).stream()
                .filter(candidate -> candidate.getStartedAt() != null)
                .sorted(Comparator.comparing(Speech::getStartedAt))
                .toList();
        if (agendaSpeeches.isEmpty()) {
            return null;
        }

        Instant baseStart = agendaSpeeches.get(0).getStartedAt();
        if (baseStart == null || speech.getStartedAt().isBefore(baseStart)) {
            return null;
        }

        int ownIndex = -1;
        for (int i = 0; i < agendaSpeeches.size(); i++) {
            Speech candidate = agendaSpeeches.get(i);
            if (candidate.getId() != null && candidate.getId().equals(speech.getId())) {
                ownIndex = i;
                break;
            }
        }
        if (ownIndex < 0) {
            return null;
        }

        Instant start = speech.getStartedAt();
        Instant end = speech.getEndedAt();
        if (end == null || !end.isAfter(start)) {
            if (ownIndex + 1 < agendaSpeeches.size()) {
                Instant nextStart = agendaSpeeches.get(ownIndex + 1).getStartedAt();
                if (nextStart != null && nextStart.isAfter(start)) {
                    end = nextStart;
                }
            }
        }
        if (end == null || !end.isAfter(start)) {
            return null;
        }

        long startSeconds = Duration.between(baseStart, start).getSeconds();
        long endSeconds = Duration.between(baseStart, end).getSeconds();
        if (startSeconds < 0 || endSeconds <= startSeconds) {
            return null;
        }
        return new ClipWindow((int) startSeconds, (int) endSeconds);
    }

    private static String deriveBundestagEmbedUrl(String videoPageUrl) {
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
        if (videoId.isBlank()) {
            return null;
        }
        return "https://www.bundestag.de/mediathekoverlay?videoid=" + videoId + "&mod=mediathek";
    }

    private static String extractBundestagVideoId(String videoPageUrl) {
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

    private static boolean isDirectMediaUrl(String value) {
        String lower = value == null ? "" : value.toLowerCase();
        return lower.contains(".mp4") || lower.contains(".m3u8");
    }

    private static String firstNonBlank(String... values) {
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

    private static String fetchRemoteEmbedHtml(String remoteUrl) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(remoteUrl))
                .header("User-Agent", "Mozilla/5.0 (compatible; MultimodalParliamentExplorer/1.0)")
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }

        String html = response.body();
        if (remoteUrl.contains("bundestag.de/mediathek/video?videoid=")) {
            return buildBundestagClipEmbedPage(html, remoteUrl);
        }
        if (!html.toLowerCase().contains("<base ")) {
            html = html.replaceFirst("(?i)<head>", "<head><base href=\"https://www.bundestag.de/\">");
        }
        html = injectBundestagVideoFocus(html);
        return html;
    }

    private static String buildBundestagClipEmbedPage(String html, String remoteUrl) {
        org.jsoup.nodes.Document source = Jsoup.parse(html, remoteUrl);
        Element player = source.selectFirst(".bt-videoplayer");
        if (player == null) {
            if (!html.toLowerCase().contains("<base ")) {
                html = html.replaceFirst("(?i)<head>", "<head><base href=\"https://www.bundestag.de/\">");
            }
            return injectBundestagVideoFocus(html);
        }

        String title = source.title();
        Element config = source.selectFirst("#globalConfigSettings");
        Elements styleLinks = source.select("head link[rel=stylesheet]");
        Elements scriptTags = source.select("script[src]");

        StringBuilder out = new StringBuilder();
        out.append("<!doctype html><html lang=\"de\"><head>");
        out.append("<meta charset=\"utf-8\">");
        out.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.append("<base href=\"https://www.bundestag.de/\">");
        out.append("<title>").append(Jsoup.parse(title == null ? "" : title).text()).append("</title>");
        for (Element link : styleLinks) {
            out.append(link.outerHtml());
        }
        out.append("""
                <style>
                html, body { margin: 0; padding: 0; background: #fff; overflow: visible; }
                body { font-family: sans-serif; }
                .mpe-player-shell { margin: 0; padding: 0; background: #fff; }
                .bt-videoplayer { margin: 0; padding: 0; width: 100%; }
                .tv1Video, .video-container { width: 100%; min-height: 560px; }
                </style>
                """);
        out.append("</head><body>");
        if (config != null) {
            out.append(config.outerHtml());
        }
        out.append("<div class=\"mpe-player-shell\">");
        out.append(player.outerHtml());
        out.append("</div>");
        for (Element script : scriptTags) {
            out.append(script.outerHtml());
        }
        out.append("""
                <script>
                (function () {
                  function reportHeight() {
                    var h = Math.max(
                      document.body ? document.body.scrollHeight : 0,
                      document.documentElement ? document.documentElement.scrollHeight : 0,
                      560
                    );
                    if (window.parent && window.parent !== window) {
                      window.parent.postMessage({ type: "mpe-bundestag-player-height", height: h }, "*");
                    }
                  }
                  window.addEventListener("load", function () {
                    reportHeight();
                    setTimeout(reportHeight, 500);
                    setTimeout(reportHeight, 1500);
                  });
                  window.addEventListener("resize", reportHeight);
                  setInterval(reportHeight, 2000);
                })();
                </script>
                """);
        out.append("</body></html>");
        return out.toString();
    }

    private static String injectBundestagVideoFocus(String html) {
        String style = """
                <style id="mpe-proxy-video-style">
                html, body { margin: 0 !important; padding: 0 !important; background: #fff !important; }
                body { overflow-x: hidden !important; }
                main, article, section, div { max-width: 100%% !important; }
                header, footer, nav, aside,
                [role="banner"], [role="navigation"], [role="contentinfo"],
                .topbar, .footer, .breadcrumb, .breadcrumbs,
                .meta-navigation, .site-header, .site-footer, .navigation {
                  display: none !important;
                }
                .bt-videoplayer, .bt-videoplayer * {
                  visibility: visible !important;
                }
                </style>
                """;
        String script = """
                <script id="mpe-proxy-video-script">
                (() => {
                  const selectors = [
                    '.bt-videoplayer',
                    'video',
                    'iframe[src*="mediathek"]',
                    '[class*="video-player"]',
                    '[id*="video-player"]',
                    '[class*="videoplayer"]',
                    '[id*="videoplayer"]',
                    '[class*="player"] video',
                    '[data-videoid]'
                  ];
                  const findTarget = () => {
                    for (const selector of selectors) {
                      const node = document.querySelector(selector);
                      if (node) return node;
                    }
                    return document.querySelector('main') || document.body;
                  };
                  const target = findTarget();
                  if (!target) return;

                  let focus = target;
                  for (let i = 0; i < 4; i++) {
                    const parent = focus.parentElement;
                    if (!parent || parent === document.body || parent === document.documentElement) break;
                    focus = parent;
                  }

                  let current = focus;
                  while (current && current.parentElement) {
                    const parent = current.parentElement;
                    for (const sibling of Array.from(parent.children)) {
                      if (sibling !== current) {
                        sibling.style.display = 'none';
                      }
                    }
                    current = parent;
                    if (current === document.body) break;
                  }

                  document.body.style.margin = '0';
                  document.body.style.background = '#fff';
                  focus.style.margin = '0 auto';
                  focus.style.maxWidth = '100%';
                  focus.style.width = '100%';
                  focus.style.padding = '0';
                  focus.style.display = 'block';

                  const video = target.tagName === 'VIDEO' ? target : target.querySelector?.('video');
                  if (video) {
                    video.style.width = '100%';
                    video.style.height = 'auto';
                    video.setAttribute('controls', 'controls');
                  }

                  const playerWrap = document.querySelector('.bt-videoplayer');
                  if (playerWrap) {
                    playerWrap.style.margin = '0';
                    playerWrap.style.padding = '0';
                    playerWrap.style.display = 'block';
                    playerWrap.style.width = '100%';
                  }

                  setTimeout(() => {
                    target.scrollIntoView({ block: 'start' });
                  }, 50);
                })();
                </script>
                """;
        String injection = style + script;
        if (html.toLowerCase().contains("</head>")) {
            return html.replaceFirst("(?i)</head>", injection + "</head>");
        }
        return injection + html;
    }

    private static String buildBundestagPlayerPage(String videoId) {
        String selector = "mpeBundestagClipPlayer_" + videoId;
        return """
                <!doctype html>
                <html lang="de">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Bundestag Clip Player</title>
                  <link rel="stylesheet" href="https://www.bundestag.de/resource/themes/bundestag/css/bundestag-596334-120.css">
                  <style>
                    html, body { margin: 0; padding: 0; background: #fff; overflow: hidden; }
                    .mpe-player-wrap { margin: 0; padding: 0; }
                    .bt-videoplayer { margin: 0; }
                    .tv1Video, .video-container { width: 100%%; min-height: 420px; }
                  </style>
                </head>
                <body>
                  <div class="mpe-player-wrap">
                    <div class="bt-videoplayer" data-nosnippet="true">
                      <div id="%s" class="tv1Video"
                           data-playertype="ondemand"
                           data-videoid="%s"
                           data-selector="%s"
                           data-language="de"></div>
                    </div>
                  </div>
                  <script src="https://www.bundestag.de/resource/themes/bundestag/js/jquery.min-826978-29.js"></script>
                  <script src="https://webtv.bundestag.de/statics/tplayer/latest/production/lib/tv1hlsplayer.js"></script>
                  <script src="https://www.bundestag.de/resource/themes/bundestag/js/commons-1031836-3.js"></script>
                  <script src="https://www.bundestag.de/resource/themes/bundestag/js/bt.ts-1031834-13.js"></script>
                  <script src="https://www.bundestag.de/resource/themes/bundestag/js/libs_wojq-596282-118.js"></script>
                  <script src="https://www.bundestag.de/resource/themes/bundestag/js/Video-596320-68.js"></script>
                </body>
                </html>
                """.formatted(selector, videoId, selector);
    }

    private record ClipWindow(int startSeconds, int endSeconds) {
    }
}
