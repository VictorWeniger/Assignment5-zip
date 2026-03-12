package org.example.service.nlp;

import org.bson.Document;
import org.example.db.DatabaseHandler;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.json.JSONArray;
import org.json.JSONObject;
import org.example.util.VideoPathConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author
 * Victor Weniger
 */

/**
 * VideoSentenceTimestampService service
 */
public class VideoSentenceTimestampService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VideoSentenceTimestampService.class);
    private static final Pattern NON_WORD = Pattern.compile("[^\\p{L}\\p{N}\\s]+");

    private final DatabaseHandler<SpeechVideo> speechVideoDatabase;
    private final boolean enabled;
    private final boolean autoDownloadEnabled;
    private final String whisperxCommand;
    private final String whisperxModel;
    private final int whisperxTimeoutSeconds;
    private final int downloadTimeoutSeconds;
    private final Path cacheRoot;
    private final Path mediaVideoRoot;
    private final Path bundledVideoRoot;
    private final HttpClient httpClient;
    private volatile boolean whisperxUnavailable;

/**
 * Constructor
 */
    public VideoSentenceTimestampService(DatabaseHandler<SpeechVideo> speechVideoDatabase) {
        this.speechVideoDatabase = speechVideoDatabase;
        this.enabled = Boolean.parseBoolean(env("MPE_ENABLE_VIDEO_TIMESTAMPS", "true"));
        this.autoDownloadEnabled = Boolean.parseBoolean(env("MPE_TIMESTAMP_AUTO_DOWNLOAD", "true"));
        this.whisperxCommand = env("MPE_WHISPERX_CMD", "whisper");
        this.whisperxModel = env("MPE_WHISPERX_MODEL", "");
        this.whisperxTimeoutSeconds = parsePositiveInt(env("MPE_WHISPERX_TIMEOUT_SECONDS", "1800"), 1800);
        this.downloadTimeoutSeconds = parsePositiveInt(env("MPE_TIMESTAMP_DOWNLOAD_TIMEOUT_SECONDS", "1800"), 1800);
        this.cacheRoot = Path.of(env("MPE_WHISPER_CACHE_DIR", ".local-data/whisper-timestamps"));
        this.mediaVideoRoot = Path.of(env("MPE_MEDIA_DIR", "data/media")).resolve("videos");
        this.bundledVideoRoot = VideoPathConfig.bundledVideoRoot();
        this.httpClient = HttpClient.newHttpClient();
    }

/**
 * Method
 */
    public void enrichSpeech(Speech speech, boolean force) {
        if (!enabled || speechVideoDatabase == null || speech == null || speech.getId() == null || speech.getId().isBlank()) {
            return;
        }
        if (speech.getNlp() == null) {
            speech.setNlp(new HashMap<>());
        }
        List<Map<String, Object>> sentenceRows = asSentenceRows(speech.getNlp().get("sentenceSentiments"), speech.getText());
        if (sentenceRows.isEmpty()) {
            speech.getNlp().put("timestampStatus", "no-sentences");
            return;
        }
        if (!force && hasTiming(sentenceRows)) {
            speech.getNlp().put("syncMode", "timed");
            speech.getNlp().put("timestampStatus", "already-present");
            return;
        }

        List<TranscriptSegment> segments = extractSegmentsFromNlp(speech.getNlp());
        String source = "nlp";
        if (segments.isEmpty()) {
            segments = loadOrGenerateSegments(speech, force);
            source = "whisperx-local";
        }
        if (segments.isEmpty()) {
            speech.getNlp().put("syncMode", "approx");
            speech.getNlp().put("timestampStatus", "missing");
            return;
        }

        speech.getNlp().put("spokenSentences", buildSpokenSentenceRows(segments));

        AlignmentStats alignment = alignSentenceRows(sentenceRows, segments, speech.getText());
        boolean timed = hasTiming(sentenceRows);
        speech.getNlp().put("sentenceSentiments", sentenceRows);
        speech.getNlp().put("syncMode", timed ? "timed" : "approx");
        speech.getNlp().put("timestampStatus", timed ? "ok" : "partial");
        speech.getNlp().put("timestampSource", source);
        speech.getNlp().put("timestampAssignedRows", alignment.assigned());
        speech.getNlp().put("timestampAlignmentScore", alignment.avgScore());
        speech.setSentenceSentiments(extractScores(sentenceRows));
    }

    private List<TranscriptSegment> loadOrGenerateSegments(Speech speech, boolean force) {
        SpeechVideo video = speechVideoDatabase.find("speech_videos", new Document("speechId", speech.getId()), SpeechVideo.class)
                .stream()
                .findFirst()
                .orElse(null);
        if (video == null) {
            return List.of();
        }
        Path videoPath = resolveLocalVideoPath(video, speech, force);
        if (videoPath == null || !Files.isRegularFile(videoPath)) {
            return List.of();
        }

        Path workDir = cacheRoot.resolve(speech.getId());
        Path cachedJson = expectedWhisperJsonPath(workDir, videoPath);
        if (!force && Files.isRegularFile(cachedJson)) {
            return parseWhisperSegments(cachedJson);
        }

        boolean generated = runWhisperx(videoPath, workDir);
        if (!generated) {
            return List.of();
        }

        Path produced = Files.isRegularFile(cachedJson) ? cachedJson : firstJsonInDirectory(workDir);
        if (produced == null) {
            return List.of();
        }
        return parseWhisperSegments(produced);
    }

    private Path resolveLocalVideoPath(SpeechVideo video, Speech speech, boolean force) {
        if (video.getLocalPath() != null && !video.getLocalPath().isBlank()) {
            Path local = Path.of(video.getLocalPath());
            if (Files.isRegularFile(local)) {
                return local;
            }
        }

        Path discovered = discoverExistingLocalVideo(video);
        if (discovered != null) {
            video.setLocalPath(discovered.toString());
            if (video.getId() != null && !video.getId().isBlank()) {
                speechVideoDatabase.replaceById("speech_videos", video.getId(), video);
            }
            return discovered;
        }

        Path cached = cachedVideoPath(speech, video);
        if (cached != null && Files.isRegularFile(cached)) {
            return cached;
        }

        if (!autoDownloadEnabled) {
            return null;
        }
        String sourceUrl = firstNonBlank(video.getStreamUrl(), video.getSourceUrl());
        if (sourceUrl == null || sourceUrl.isBlank() || !isDirectMediaUrl(sourceUrl)) {
            return null;
        }

        Path downloadPath = downloadTargetPath(speech, sourceUrl);
        if (downloadPath == null) {
            return null;
        }
        if (!force && Files.isRegularFile(downloadPath)) {
            return downloadPath;
        }
        boolean downloaded = downloadMedia(sourceUrl, downloadPath);
        if (!downloaded) {
            return null;
        }

        video.setLocalPath(downloadPath.toString());
        if (video.getId() != null && !video.getId().isBlank()) {
            speechVideoDatabase.replaceById("speech_videos", video.getId(), video);
        }
        return downloadPath;
    }

    private Path discoverExistingLocalVideo(SpeechVideo video) {
        if (video == null) {
            return null;
        }
        Path found = findByVideoIdInMediaDir(video);
        if (found != null) {
            return found;
        }
        return findInBundledVideos(video);
    }

    private Path findByVideoIdInMediaDir(SpeechVideo video) {
        if (!Files.isDirectory(mediaVideoRoot) || video.getId() == null || video.getId().isBlank()) {
            return null;
        }
        Path directMp4 = mediaVideoRoot.resolve(video.getId() + ".mp4");
        if (Files.isRegularFile(directMp4)) {
            return directMp4;
        }
        try (var stream = Files.list(mediaVideoRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(video.getId() + ".")
                            || path.getFileName().toString().equals(video.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private Path findInBundledVideos(SpeechVideo video) {
        if (!Files.isDirectory(bundledVideoRoot)) {
            return null;
        }
        String videoId = extractBundestagVideoId(firstNonBlank(
                video.getVideoPageUrl(),
                video.getSourceUrl(),
                video.getStreamUrl()
        ));
        if (videoId == null || videoId.isBlank()) {
            return null;
        }
        try (var paths = Files.walk(bundledVideoRoot, 6)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> VideoPathConfig.fileNameMatchesVideoId(path.getFileName().toString(), videoId))
                    .findFirst()
                    .orElse(null);
        } catch (IOException ignored) {
            return null;
        }
    }

    private Path cachedVideoPath(Speech speech, SpeechVideo video) {
        if (speech == null || speech.getId() == null || speech.getId().isBlank()) {
            return null;
        }
        if (video == null) {
            return null;
        }
        String sourceUrl = firstNonBlank(video.getStreamUrl(), video.getSourceUrl());
        if (sourceUrl == null || sourceUrl.isBlank()) {
            return null;
        }
        Path target = downloadTargetPath(speech, sourceUrl);
        return Files.isRegularFile(target) ? target : null;
    }

    private Path downloadTargetPath(Speech speech, String sourceUrl) {
        if (speech == null || speech.getId() == null || speech.getId().isBlank()) {
            return null;
        }
        String extension = extensionFromUrl(sourceUrl, ".mp4");
        if (extension.isBlank()) {
            return null;
        }
        return cacheRoot.resolve(speech.getId()).resolve("video" + extension);
    }

    private boolean isDirectMediaUrl(String url) {
        String ext = extensionFromUrl(url, "");
        return ".mp4".equalsIgnoreCase(ext)
                || ".m4a".equalsIgnoreCase(ext)
                || ".mp3".equalsIgnoreCase(ext)
                || ".wav".equalsIgnoreCase(ext);
    }

    private boolean downloadMedia(String sourceUrl, Path destination) {
        try {
            Files.createDirectories(destination.getParent());
            HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
                    .timeout(Duration.ofSeconds(Math.max(1, downloadTimeoutSeconds)))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.warn("Video download failed (HTTP {}) for {}", response.statusCode(), sourceUrl);
                return false;
            }
            try (InputStream input = response.body()) {
                Files.copy(input, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Video download interrupted for {}", sourceUrl);
            return false;
        } catch (Exception e) {
            LOGGER.warn("Video download failed for {}: {}", sourceUrl, e.getMessage());
            return false;
        }
    }

    private String extensionFromUrl(String url, String fallback) {
        if (url == null || url.isBlank()) {
            return fallback;
        }
        int q = url.indexOf('?');
        String clean = q >= 0 ? url.substring(0, q) : url;
        int dot = clean.lastIndexOf('.');
        if (dot < 0 || dot < clean.lastIndexOf('/')) {
            return fallback;
        }
        String ext = clean.substring(dot);
        return ext.length() > 8 ? fallback : ext;
    }

    private String extractBundestagVideoId(String url) {
        if (url == null || url.isBlank() || !url.contains("videoid=")) {
            return null;
        }
        int start = url.indexOf("videoid=");
        if (start < 0) {
            return null;
        }
        String rest = url.substring(start + "videoid=".length());
        int end = rest.indexOf('&');
        String videoId = end >= 0 ? rest.substring(0, end) : rest;
        return videoId.isBlank() ? null : videoId;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private boolean runWhisperx(Path videoPath, Path outputDir) {
        if (whisperxUnavailable) {
            return false;
        }
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            LOGGER.warn("Could not create WhisperX output directory {}: {}", outputDir, e.getMessage());
            return false;
        }

        List<String> command = buildTranscriptionCommand(videoPath, outputDir);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(whisperxTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warn("WhisperX timed out after {}s for {}", whisperxTimeoutSeconds, videoPath.getFileName());
                return false;
            }
            if (process.exitValue() != 0) {
                LOGGER.warn("WhisperX failed (exit={}) for {}. Output: {}",
                        process.exitValue(), videoPath.getFileName(), shorten(output, 500));
                return false;
            }
            return true;
        } catch (IOException e) {
            LOGGER.warn("WhisperX command '{}' not available or failed to start: {}", whisperxCommand, e.getMessage());
            whisperxUnavailable = true;
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("WhisperX execution interrupted: {}", e.getMessage());
            return false;
        }
    }

    private List<String> buildTranscriptionCommand(Path videoPath, Path outputDir) {
        List<String> command = new ArrayList<>();
        command.add(whisperxCommand);
        command.add(videoPath.toString());

        String base = commandBaseName(whisperxCommand);
        boolean usePlainWhisper = "whisper".equals(base);

        if (usePlainWhisper) {
            command.add("--language");
            command.add("de");
            command.add("--output_format");
            command.add("json");
            command.add("--output_dir");
            command.add(outputDir.toString());
            command.add("--fp16");
            command.add("False");
            if (whisperxModel != null && !whisperxModel.isBlank()) {
                command.add("--model");
                command.add(whisperxModel);
            }
            return command;
        }

        command.add("--language");
        command.add("de");
        command.add("--output_format");
        command.add("json");
        command.add("--output_dir");
        command.add(outputDir.toString());
        command.add("--compute_type");
        command.add("int8");
        command.add("--device");
        command.add("cpu");
        if (whisperxModel != null && !whisperxModel.isBlank()) {
            command.add("--model");
            command.add(whisperxModel);
        }
        return command;
    }

    private static String commandBaseName(String command) {
        if (command == null || command.isBlank()) {
            return "";
        }
        String normalized = command.trim();
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        return (slash >= 0 ? normalized.substring(slash + 1) : normalized).toLowerCase(Locale.ROOT);
    }

    private List<TranscriptSegment> parseWhisperSegments(Path jsonPath) {
        try {
            String raw = Files.readString(jsonPath, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(raw);
            Object candidate = root.opt("segments");
            if (!(candidate instanceof JSONArray array)) {
                return List.of();
            }
            List<TranscriptSegment> segments = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                Object obj = array.get(i);
                if (!(obj instanceof JSONObject seg)) {
                    continue;
                }
                Double start = asDoubleOrNull(seg.opt("start"));
                Double end = asDoubleOrNull(seg.opt("end"));
                String text = seg.optString("text", "");
                if (start == null && end == null && text.isBlank()) {
                    continue;
                }
                segments.add(new TranscriptSegment(start, end, text));
            }
            return segments;
        } catch (Exception e) {
            LOGGER.warn("Failed parsing WhisperX JSON {}: {}", jsonPath, e.getMessage());
            return List.of();
        }
    }

    private List<TranscriptSegment> extractSegmentsFromNlp(Map<String, Object> nlp) {
        List<TranscriptSegment> segments = new ArrayList<>();
        collectSegments(nlp.get("segments"), segments);
        collectSegments(nlp.get("transcriptSegments"), segments);
        collectSegments(nlp.get("whisperSegments"), segments);
        Object transcript = nlp.get("transcript");
        if (transcript instanceof Map<?, ?> map) {
            collectSegments(map.get("segments"), segments);
        }
        return segments;
    }

    private void collectSegments(Object value, List<TranscriptSegment> target) {
        if (value instanceof List<?> list) {
            for (Object entry : list) {
                TranscriptSegment segment = toSegment(entry);
                if (segment != null) {
                    target.add(segment);
                }
            }
        }
    }

    private TranscriptSegment toSegment(Object value) {
        if (value instanceof Map<?, ?> map) {
            Double start = firstFinite(
                    asDoubleOrNull(map.get("start")),
                    asDoubleOrNull(map.get("startTime")),
                    asDoubleOrNull(map.get("t0"))
            );
            Double end = firstFinite(
                    asDoubleOrNull(map.get("end")),
                    asDoubleOrNull(map.get("endTime")),
                    asDoubleOrNull(map.get("t1"))
            );
            String text = asString(map.get("text"), "");
            if (text.isBlank()) {
                text = asString(map.get("sentence"), "");
            }
            if (start == null && end == null && text.isBlank()) {
                return null;
            }
            return new TranscriptSegment(start, end, text);
        }
        return null;
    }

    private AlignmentStats alignSentenceRows(List<Map<String, Object>> sentenceRows, List<TranscriptSegment> segments, String speechText) {
        if (sentenceRows == null || sentenceRows.isEmpty() || segments == null || segments.isEmpty()) {
            return new AlignmentStats(0, 0.0);
        }
        int assignedByOffset = assignTimingByOffsetOverlap(sentenceRows, segments, speechText);
        double avgScore = sentenceRows.isEmpty() ? 0.0 : (double) assignedByOffset / (double) sentenceRows.size();
        return new AlignmentStats(assignedByOffset, avgScore);
    }

    private int assignTimingByOffsetOverlap(List<Map<String, Object>> sentenceRows, List<TranscriptSegment> segments, String speechText) {
        if (speechText == null || speechText.isBlank() || sentenceRows == null || sentenceRows.isEmpty() || segments == null || segments.isEmpty()) {
            return 0;
        }

        List<MappedSegment> mapped = mapSegmentsToSpeechOffsets(segments, speechText);
        if (mapped.isEmpty()) {
            return 0;
        }
        mapped.sort(Comparator.comparingInt(MappedSegment::begin));

        int assigned = 0;
        for (Map<String, Object> row : sentenceRows) {
            int sb = asInt(row.get("begin"), -1);
            int se = asInt(row.get("end"), -1);
            if (sb < 0 || se <= sb || se > speechText.length()) {
                continue;
            }
            Double minT0 = null;
            Double maxT1 = null;

            for (MappedSegment seg : mapped) {
                if (seg.end() <= sb) {
                    continue;
                }
                if (seg.begin() >= se) {
                    break;
                }
                if (seg.t0() != null) {
                    minT0 = minT0 == null ? seg.t0() : Math.min(minT0, seg.t0());
                }
                if (seg.t1() != null) {
                    maxT1 = maxT1 == null ? seg.t1() : Math.max(maxT1, seg.t1());
                }
            }

            if (minT0 != null) {
                row.put("t0", minT0);
            }
            if (maxT1 != null) {
                row.put("t1", maxT1);
            }
            if (minT0 != null || maxT1 != null) {
                assigned++;
            }
        }
        return assigned;
    }

    private List<MappedSegment> mapSegmentsToSpeechOffsets(List<TranscriptSegment> segments, String speechText) {
        List<MappedSegment> mapped = new ArrayList<>();
        if (segments == null || segments.isEmpty() || speechText == null || speechText.isBlank()) {
            return mapped;
        }

        String baseText = speechText;
        int rawCursor = 0;
        NormalizedText normalizedBase = normalizeWithMapping(baseText);
        int normalizedCursor = 0;

        for (TranscriptSegment seg : segments) {
            String segText = asString(seg.text(), "").trim();
            if (segText.isBlank()) {
                continue;
            }

            int begin = baseText.indexOf(segText, Math.max(0, rawCursor));
            int end = -1;
            if (begin < 0) {
                String normalizedSegment = normalize(segText);
                if (!normalizedSegment.isBlank()) {
                    int idxNorm = normalizedBase.value().indexOf(normalizedSegment, Math.max(0, normalizedCursor));
                    if (idxNorm >= 0) {
                        int idxNormEnd = idxNorm + normalizedSegment.length() - 1;
                        if (idxNorm < normalizedBase.rawIndexByNormalizedIndex().length
                                && idxNormEnd < normalizedBase.rawIndexByNormalizedIndex().length) {
                            begin = normalizedBase.rawIndexByNormalizedIndex()[idxNorm];
                            end = normalizedBase.rawIndexByNormalizedIndex()[idxNormEnd] + 1;
                            normalizedCursor = Math.min(normalizedBase.value().length(), idxNorm + normalizedSegment.length());
                        }
                    }
                }
            }
            if (begin < 0) {
                continue;
            }

            if (end <= begin) {
                end = Math.min(baseText.length(), begin + segText.length());
            } else {
                end = Math.min(baseText.length(), end);
            }
            if (end <= begin) {
                continue;
            }

            rawCursor = Math.min(baseText.length(), end + 1);
            mapped.add(new MappedSegment(begin, end, seg.start(), seg.end()));
        }
        return mapped;
    }

    private NormalizedText normalizeWithMapping(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return new NormalizedText("", new int[0]);
        }
        String lower = rawText.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        List<Integer> rawIndexByNorm = new ArrayList<>(lower.length());

        boolean previousWasSpace = true;
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            boolean keepAsToken = Character.isLetterOrDigit(ch);
            if (keepAsToken) {
                normalized.append(ch);
                rawIndexByNorm.add(i);
                previousWasSpace = false;
            } else if (!previousWasSpace) {
                normalized.append(' ');
                rawIndexByNorm.add(i);
                previousWasSpace = true;
            }
        }

        int len = normalized.length();
        while (len > 0 && normalized.charAt(len - 1) == ' ') {
            normalized.deleteCharAt(len - 1);
            rawIndexByNorm.remove(rawIndexByNorm.size() - 1);
            len--;
        }

        int[] mapping = new int[rawIndexByNorm.size()];
        for (int i = 0; i < rawIndexByNorm.size(); i++) {
            mapping[i] = rawIndexByNorm.get(i);
        }
        return new NormalizedText(normalized.toString(), mapping);
    }

    private List<Map<String, Object>> asSentenceRows(Object value, String speechText) {
        if (!(value instanceof List<?> list)) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.putAll((Map<String, Object>) map);
            String sentence = asString(row.get("sentence"), "");
            if (sentence.isBlank()) {
                int begin = asInt(row.get("begin"), -1);
                int end = asInt(row.get("end"), -1);
                if (speechText != null && begin >= 0 && end > begin && end <= speechText.length()) {
                    row.put("sentence", speechText.substring(begin, end));
                }
            }
            rows.add(row);
        }
        rows.sort(Comparator.comparingInt(m -> asInt(m.get("begin"), Integer.MAX_VALUE)));
        return rows;
    }

    private boolean hasTiming(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        int total = rows.size();
        int withT0 = 0;
        double lastT0 = -1;
        boolean monotonic = true;

        for (Map<String, Object> row : rows) {
            if (isFinite(row.get("t0"))) {
                double t0 = asDouble(row.get("t0"), Double.NaN);
                if (Double.isFinite(t0)) {
                    withT0++;
                    if (lastT0 >= 0 && t0 + 0.05 < lastT0) {
                        monotonic = false;
                    }
                    lastT0 = t0;
                }
            }
        }

        double coverage = total == 0 ? 0.0 : (double) withT0 / total;
        return coverage >= 0.7 && monotonic;
    }

    private record AlignmentStats(int assigned, double avgScore) {
    }

    private record MappedSegment(int begin, int end, Double t0, Double t1) {
    }

    private record NormalizedText(String value, int[] rawIndexByNormalizedIndex) {
    }

    private List<Double> extractScores(List<Map<String, Object>> rows) {
        List<Double> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object score = row.get("score");
            if (score instanceof Number n) {
                values.add(n.doubleValue());
            }
        }
        return values;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return NON_WORD.matcher(text.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<Map<String, Object>> buildSpokenSentenceRows(List<TranscriptSegment> segments) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (segments == null || segments.isEmpty()) {
            return out;
        }
        for (TranscriptSegment segment : segments) {
            String text = asString(segment.text(), "").replaceAll("\\s+", " ").trim();
            Double start = segment.start();
            Double end = segment.end();
            if (text.isBlank() || start == null || end == null || !Double.isFinite(start) || !Double.isFinite(end) || end <= start) {
                continue;
            }

            List<String> sentenceParts = splitIntoSentences(text);
            if (sentenceParts.isEmpty()) {
                continue;
            }
            int totalLen = sentenceParts.stream().mapToInt(String::length).sum();
            if (totalLen <= 0) {
                continue;
            }

            double span = end - start;
            double cursor = start;
            for (int i = 0; i < sentenceParts.size(); i++) {
                String sentence = sentenceParts.get(i);
                double fraction = (double) sentence.length() / (double) totalLen;
                double t0 = cursor;
                double t1 = (i == sentenceParts.size() - 1) ? end : (cursor + (span * fraction));
                if (t1 < t0) {
                    t1 = t0;
                }
                cursor = t1;

                Map<String, Object> row = new HashMap<>();
                row.put("sentence", sentence);
                row.put("t0", t0);
                row.put("t1", t1);
                row.put("start", t0);
                row.put("end", t1);
                out.add(row);
            }
        }
        out.sort(Comparator.comparingDouble(m -> asDouble(m.get("t0"), Double.POSITIVE_INFINITY)));
        return out;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return parts;
        }
        String[] raw = text.split("(?<=[.!?])\\s+");
        for (String candidate : raw) {
            String sentence = candidate == null ? "" : candidate.trim();
            if (!sentence.isBlank()) {
                parts.add(sentence);
            }
        }
        if (parts.isEmpty()) {
            parts.add(text.trim());
        }
        return parts;
    }

    private Path expectedWhisperJsonPath(Path outputDir, Path videoPath) {
        String fileName = videoPath.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String stem = dot > 0 ? fileName.substring(0, dot) : fileName;
        return outputDir.resolve(stem + ".json");
    }

    private Path firstJsonInDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            return null;
        }
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .sorted()
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static int parsePositiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String shorten(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen) + "...";
    }

    private static Double firstFinite(Double... values) {
        for (Double value : values) {
            if (value != null && Double.isFinite(value)) {
                return value;
            }
        }
        return null;
    }

    private static double asDouble(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static Double asDoubleOrNull(Object value) {
        if (value == null) {
            return null;
        }
        double parsed = asDouble(value, Double.NaN);
        return Double.isFinite(parsed) ? parsed : null;
    }

    private static boolean isFinite(Object value) {
        Double parsed = asDoubleOrNull(value);
        return parsed != null && Double.isFinite(parsed);
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String asString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String s = String.valueOf(value);
        return s == null ? fallback : s;
    }

    private record TranscriptSegment(Double start, Double end, String text) {
    }
}
