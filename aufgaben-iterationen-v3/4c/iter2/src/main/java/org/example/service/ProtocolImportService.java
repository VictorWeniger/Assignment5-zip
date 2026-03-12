package org.example.service;

import org.example.db.DatabaseHandler;
import org.example.model.Deputy;
import org.example.model.ImageMetadata;
import org.example.model.ProtocolDocument;
import org.example.model.ProtocolSession;
import org.example.model.Speech;
import org.example.model.SpeechVideo;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author
 * Victor Weniger
 */

/**
 * ProtocolImportService service
 */
public class ProtocolImportService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolImportService.class);

    private final BundestagProtocolDownloader downloader;
    private final DatabaseHandler<ProtocolDocument> protocolDatabase;
    private final DatabaseHandler<ProtocolSession> sessionDatabase;
    private final DatabaseHandler<Speech> speechDatabase;
    private final DatabaseHandler<Deputy> deputyDatabase;
    private final DatabaseHandler<SpeechVideo> speechVideoDatabase;
    private final DeputyImageEnrichmentService deputyImageEnrichmentService;
    private final MediaAssetDownloadService mediaAssetDownloadService;
    private final XmlProtocolParser parser;
    private final AtomicBoolean importRunning = new AtomicBoolean(false);

/**
 * Constructor
 */
    public ProtocolImportService(
            BundestagProtocolDownloader downloader,
            DatabaseHandler<ProtocolDocument> protocolDatabase,
            DatabaseHandler<ProtocolSession> sessionDatabase,
            DatabaseHandler<Speech> speechDatabase,
            DatabaseHandler<Deputy> deputyDatabase,
            DatabaseHandler<SpeechVideo> speechVideoDatabase,
            DeputyImageEnrichmentService deputyImageEnrichmentService,
            MediaAssetDownloadService mediaAssetDownloadService,
            XmlProtocolParser parser
    ) {
        this.downloader = downloader;
        this.protocolDatabase = protocolDatabase;
        this.sessionDatabase = sessionDatabase;
        this.speechDatabase = speechDatabase;
        this.deputyDatabase = deputyDatabase;
        this.speechVideoDatabase = speechVideoDatabase;
        this.deputyImageEnrichmentService = deputyImageEnrichmentService;
        this.mediaAssetDownloadService = mediaAssetDownloadService;
        this.parser = parser;
    }

/**
 * Method
 */
    public ImportSummary importMissingProtocols() {
        return importProtocols(ImportOptions.defaultOptions());
    }

/**
 * Method
 */
    public List<ImportCandidate> previewImportCandidates(ImportOptions options) {
        try {
            List<String> discoveredLinks = downloader.fetchProtocolXmlLinks();
            LOGGER.info("Discovered {} raw protocol XML links for preview", discoveredLinks.size());
            List<String> xmlLinks = filterLinks(discoveredLinks, options);
            LOGGER.info("Preview retained {} protocol XML links after filtering", xmlLinks.size());
            return xmlLinks.stream().map(link -> {
                try {
                    ProtocolIdParser.ParsedProtocolId parsed = ProtocolIdParser.parse(link);
                    boolean exists = protocolDatabase.count("protocols", new Document("id", parsed.protocolId())) > 0;
                    return new ImportCandidate(parsed.protocolId(), parsed.legislativePeriod(), parsed.sessionNumber(), link, exists);
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            }).filter(candidate -> candidate != null).collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error("Could not preview import candidates", e);
            return List.of();
        }
    }

/**
 * Method
 */
    public ImportSummary importSingleProtocol(String protocolId, boolean forceReimportExisting) {
        if (!importRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("A protocol import is already running");
        }
        try {
            List<String> xmlLinks = downloader.fetchProtocolXmlLinks();
            LOGGER.info("Discovered {} raw protocol XML links for single import {}", xmlLinks.size(), protocolId);
            for (String xmlLink : xmlLinks) {
                ProtocolIdParser.ParsedProtocolId parsed;
                try {
                    parsed = ProtocolIdParser.parse(xmlLink);
                } catch (IllegalArgumentException ex) {
                    continue;
                }

                if (!parsed.protocolId().equals(protocolId)) {
                    continue;
                }

                ImportOptions options = new ImportOptions(parsed.legislativePeriod(), 1, forceReimportExisting);
                return importProtocolsWithFixedLinks(options, List.of(xmlLink));
            }
            return new ImportSummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
        } catch (IOException e) {
            LOGGER.error("Could not import single protocol {}", protocolId, e);
            return new ImportSummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
        } finally {
            importRunning.set(false);
        }
    }

/**
 * Method
 */
    public ImportSummary importProtocols(ImportOptions options) {
        if (!importRunning.compareAndSet(false, true)) {
            throw new IllegalStateException("A protocol import is already running");
        }
        try {
            List<String> discoveredLinks = downloader.fetchProtocolXmlLinks();
            LOGGER.info("Discovered {} raw protocol XML links for import", discoveredLinks.size());
            List<String> xmlLinks = prefilterExistingProtocols(filterLinks(discoveredLinks, options), options);
            LOGGER.info(
                    "Import retained {} protocol XML links after filtering (period={}, limit={}, force={})",
                    xmlLinks.size(),
                    options.legislativePeriodFilter(),
                    options.maxProtocols(),
                    options.forceReimportExisting()
            );
            if (xmlLinks.isEmpty()) {
                LOGGER.warn("No protocol XML links available for import after discovery/filtering");
            }
            return importProtocolsWithFixedLinks(options, xmlLinks);
        } catch (IOException e) {
            LOGGER.error("Protocol import failed", e);
            return new ImportSummary(0, 0, 0, 0, 0, 0, 0, 0, 0);
        } finally {
            importRunning.set(false);
        }
    }

    private List<String> prefilterExistingProtocols(List<String> xmlLinks, ImportOptions options) {
        if (options.forceReimportExisting() || xmlLinks.isEmpty()) {
            return xmlLinks;
        }

        Set<String> existingProtocolIds = protocolDatabase.find("protocols", new Document(), ProtocolDocument.class).stream()
                .map(ProtocolDocument::getId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (existingProtocolIds.isEmpty()) {
            return xmlLinks;
        }

        List<String> filtered = xmlLinks.stream()
                .filter(link -> {
                    try {
                        return !existingProtocolIds.contains(ProtocolIdParser.parse(link).protocolId());
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                })
                .toList();

        int skippedExisting = xmlLinks.size() - filtered.size();
        if (skippedExisting > 0) {
            LOGGER.info("Prefilter skipped {} already imported protocol links before download", skippedExisting);
        }
        return filtered;
    }

    private ImportSummary importProtocolsWithFixedLinks(ImportOptions options, List<String> xmlLinks) {
        int importedProtocols = 0;
        int upsertedSessions = 0;
        int upsertedSpeeches = 0;
        int upsertedDeputies = 0;
        int upsertedVideos = 0;
        int enrichedDeputyImages = 0;
        int skippedInvalidSpeeches = 0;
        int skippedInvalidDeputies = 0;
        int skippedInvalidVideos = 0;

        for (String xmlLink : xmlLinks) {
            ProtocolIdParser.ParsedProtocolId parsedId;
            try {
                parsedId = ProtocolIdParser.parse(xmlLink);
            } catch (IllegalArgumentException ex) {
                LOGGER.debug("Skipping URL without parseable protocol id: {}", xmlLink);
                continue;
            }

            boolean exists = protocolDatabase.count("protocols", new Document("id", parsedId.protocolId())) > 0;
            if (exists && !options.forceReimportExisting()) {
                continue;
            }

            String xml;
            try {
                xml = downloader.downloadXml(xmlLink);
            } catch (IOException e) {
                LOGGER.error("Failed to download xml for {}", xmlLink, e);
                continue;
            }
            ProtocolDocument protocol = new ProtocolDocument();
            protocol.setId(parsedId.protocolId());
            protocol.setLegislativePeriod(parsedId.legislativePeriod());
            protocol.setSessionNumber(parsedId.sessionNumber());
            protocol.setSourceUrl(xmlLink);
            protocol.setRawXml(xml);
            protocol.setImportedAt(Instant.now());
            protocolDatabase.replaceById("protocols", protocol.getId(), protocol);
            importedProtocols++;

            XmlProtocolParser.ParsedProtocol parsedProtocol = parser.parse(
                    parsedId.protocolId(),
                    parsedId.legislativePeriod(),
                    xml
            );
            ValidationResult validationResult = validateParsedProtocol(parsedProtocol, parsedId.protocolId());

            sessionDatabase.replaceById("sessions", validationResult.session().getId(), validationResult.session());
            upsertedSessions++;

            for (Speech speech : validationResult.speeches()) {
                speechDatabase.replaceById("speeches", speech.getId(), speech);
                upsertedSpeeches++;
            }
            skippedInvalidSpeeches += validationResult.invalidSpeechCount();

            for (Deputy deputy : validationResult.deputies()) {
                deputyImageEnrichmentService.enrichWithProfileImage(deputy);
                for (ImageMetadata image : deputy.getImages()) {
                    mediaAssetDownloadService.downloadDeputyImage(image, deputy.getId());
                    enrichedDeputyImages++;
                }

                deputyDatabase.replaceById("deputies", deputy.getId(), deputy);
                upsertedDeputies++;
            }
            skippedInvalidDeputies += validationResult.invalidDeputyCount();

            for (SpeechVideo speechVideo : validationResult.videos()) {
                mediaAssetDownloadService.downloadSpeechVideo(speechVideo);
                speechVideoDatabase.replaceById("speech_videos", speechVideo.getId(), speechVideo);
                upsertedVideos++;
            }
            skippedInvalidVideos += validationResult.invalidVideoCount();
        }

        return new ImportSummary(
                importedProtocols,
                upsertedSessions,
                upsertedSpeeches,
                upsertedDeputies,
                upsertedVideos,
                enrichedDeputyImages,
                skippedInvalidSpeeches,
                skippedInvalidDeputies,
                skippedInvalidVideos
        );
    }

    private ValidationResult validateParsedProtocol(XmlProtocolParser.ParsedProtocol parsedProtocol, String protocolId) {
        ProtocolSession session = parsedProtocol.session();
        if (session.getId() == null || session.getId().isBlank()) {
            session.setId("session-" + protocolId);
        }
        if (session.getProtocolId() == null || session.getProtocolId().isBlank()) {
            session.setProtocolId(protocolId);
        }

        List<Speech> validSpeeches = parsedProtocol.speeches().stream()
                .filter(speech -> speech != null && speech.getId() != null && !speech.getId().isBlank())
                .peek(speech -> {
                    if (speech.getProtocolId() == null || speech.getProtocolId().isBlank()) {
                        speech.setProtocolId(protocolId);
                    }
                    if (speech.getSessionId() == null || speech.getSessionId().isBlank()) {
                        speech.setSessionId(session.getId());
                    }
                    if (speech.getText() == null) {
                        speech.setText("");
                    }
                })
                .collect(Collectors.toList());
        int invalidSpeechCount = parsedProtocol.speeches().size() - validSpeeches.size();

        List<Deputy> validDeputies = parsedProtocol.deputies().stream()
                .filter(deputy -> deputy != null && deputy.getId() != null && !deputy.getId().isBlank())
                .collect(Collectors.toList());
        int invalidDeputyCount = parsedProtocol.deputies().size() - validDeputies.size();

        Set<String> validSpeechIds = new HashSet<>();
        for (Speech speech : validSpeeches) {
            validSpeechIds.add(speech.getId());
        }
        List<SpeechVideo> validVideos = parsedProtocol.videos().stream()
                .filter(video -> video != null
                        && video.getId() != null
                        && !video.getId().isBlank()
                        && video.getSpeechId() != null
                        && validSpeechIds.contains(video.getSpeechId())
                        && video.getSourceUrl() != null
                        && !video.getSourceUrl().isBlank())
                .collect(Collectors.toList());
        int invalidVideoCount = parsedProtocol.videos().size() - validVideos.size();

        return new ValidationResult(
                session,
                validSpeeches,
                validDeputies,
                validVideos,
                invalidSpeechCount,
                invalidDeputyCount,
                invalidVideoCount
        );
    }

    private List<String> filterLinks(List<String> xmlLinks, ImportOptions options) {
        List<String> filtered = xmlLinks.stream()
                .filter(link -> {
                    if (options.legislativePeriodFilter() == null) {
                        return true;
                    }
                    try {
                        ProtocolIdParser.ParsedProtocolId id = ProtocolIdParser.parse(link);
                        return id.legislativePeriod() == options.legislativePeriodFilter();
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                })
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        if (options.legislativePeriodFilter() != null && filtered.isEmpty()) {
            LOGGER.warn(
                    "No protocol XML links matched legislative period {}. Sample discovered links: {}",
                    options.legislativePeriodFilter(),
                    xmlLinks.stream().limit(5).collect(Collectors.toList())
            );
        }

        if (options.maxProtocols() > 0 && filtered.size() > options.maxProtocols()) {
            return filtered.subList(0, options.maxProtocols());
        }
        return filtered;
    }

/**
 * ImportSummary service
 */
    public record ImportSummary(
            int importedProtocols,
            int upsertedSessions,
            int upsertedSpeeches,
            int upsertedDeputies,
            int upsertedVideos,
            int enrichedDeputyImages,
            int skippedInvalidSpeeches,
            int skippedInvalidDeputies,
            int skippedInvalidVideos
    ) {
    }

/**
 * ImportOptions service
 */
    public record ImportOptions(Integer legislativePeriodFilter, int maxProtocols, boolean forceReimportExisting) {

/**
 * Method
 */
        public static ImportOptions defaultOptions() {
            return new ImportOptions(null, 0, false);
        }
    }

/**
 * ImportCandidate service
 */
    public record ImportCandidate(
            String protocolId,
            int legislativePeriod,
            int sessionNumber,
            String sourceUrl,
            boolean alreadyImported
    ) {
    }

    private record ValidationResult(
            ProtocolSession session,
            List<Speech> speeches,
            List<Deputy> deputies,
            List<SpeechVideo> videos,
            int invalidSpeechCount,
            int invalidDeputyCount,
            int invalidVideoCount
    ) {
    }
}
