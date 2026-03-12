# Full Code Commentary (Assignment5)

Diese Datei ist die zentrale, menschenlesbare Erklaerung fuer den gesamten aktiven Code im Projektordner.

## Backend Entry
- `src/main/java/org/example/Main/Main.java`: very small launcher, delegiert direkt an `Application.main`.
- `src/main/java/org/example/Application.java`: kompletter Bootstrap (Config laden, Services bauen, Routes registrieren, Shutdown-Hook).

## API Layer (`src/main/java/org/example/api`)
- `AgendaVideoCandidate.java`: DTO fuer Vorschlaege, welche Agenda-Items gute Video-Kandidaten sind.
- `ApiDocsController.java`: liefert schnelle API-Dokumentationsseite/Route-Liste.
- `ExportController.java`: TeX/PDF-Export-Endpunkte und Template-Integration.
- `FrontendController.java`: serverseitige Page-Routen fuer Freemarker-Templates.
- `HealthController.java`: einfacher Healthcheck-Endpunkt.
- `ImportController.java`: Startpunkte fuer Protokollimport + Videoimport.
- `NlpController.java`: NLP-Run, NLP-Statistik, XMI-Import-Endpunkte.
- `ProtocolController.java`: grosse Read-API fuer Protokolle, Reden, Videos, Detailansichten und Aggregationen.
- `ProtocolSummary.java`: reduzierte API-Sicht auf Protokoll-Daten.
- `SpeechDetailResponse.java`: zusammengesetzte Antwort fuer Speech-Detail (Speech+Speaker+Video+Clipfenster).
- `SpeechQueryFilterBuilder.java`: baut Mongo-Filter fuer kombinierte Speech-Query-Parameter.
- `SwaggerController.java`: Swagger/OpenAPI-Auslieferung.
- `TemplateController.java`: CRUD/Seed fuer Export-Templates.

## Config Layer (`src/main/java/org/example/config`)
- `AppConfig.java`: zentrale Runtime-Config aus ENV + Credentials-Datei.
- `CredentialsFileLoader.java`: liest optionale `.properties`-Datei fuer lokale Defaults.
- `DatabaseConfig.java`: Mongo-Connection-Details und Fallback-Logik.
- `NlpConfig.java`: NLP-Engine-Modus + DUUI-Endpoint/Token/Timeout.

## DB Layer (`src/main/java/org/example/db`)
- `DatabaseHandler.java`: generisches DB-Interface.
- `MongoDatabaseHandler.java`: konkrete Mongo-Implementierung des Interfaces.

## Domain Models (`src/main/java/org/example/model`)
- `Comment.java`: Zwischenruf/Kommentar-Datenstruktur.
- `Deputy.java`: Abgeordnetenprofil inkl. Bilder und Fraktion.
- `DeputyRole.java`: Enum fuer Rollen.
- `ExportTemplate.java`: editierbare TeX-Templatefragmente.
- `Identifiable.java`: gemeinsames ID-Interface fuer persistente Objekte.
- `ImageMetadata.java`: Bildquelle, lokale Datei, MIME, Copyright.
- `ParliamentaryGroup.java`: Fraktionsdaten.
- `ProtocolDocument.java`: rohes/strukturiertes Plenarprotokoll-Dokument.
- `ProtocolSession.java`: Sitzungs-Metadaten + Agenda.
- `Speech.java`: zentrale Rede-Entity inkl. NLP-Metadaten und Processing-Status.
- `SpeechVideo.java`: Video-Metadaten pro Rede.

## Service Layer (`src/main/java/org/example/service`)
- `AgendaVideoImportService.java`: matcht Agenda-Items auf Bundestag-Mediathek und schreibt `SpeechVideo`.
- `BundestagProtocolDownloader.java`: lädt Protokollquellen vom Bundestag.
- `DatabaseIndexInitializer.java`: erstellt benoetigte Mongo-Indizes beim Start.
- `DeputyImageEnrichmentService.java`: versucht Bundestag-Bilder fuer Abgeordnete anzureichern.
- `ImportScheduler.java`: periodischer Auto-Import.
- `MediaAssetDownloadService.java`: optionaler Download lokaler Medien (Video/Bilder).
- `NlpAnnotationImportService.java`: importiert professorseitige XMI/XMI.GZ-NLP-Daten.
- `NlpProcessingService.java`: orchestriert NLP-Engine-Lauf, Text-Normalisierung, Timestamp-Enrichment, CAS-Serialisierung.
- `ProtocolIdParser.java`: Hilfsparser fuer Protokoll-IDs.
- `ProtocolImportService.java`: End-to-End-Protokollimport (Dokumente, Sessions, Speeches, Deputies, Videos).
- `XmlProtocolParser.java`: XML-Parsing in interne Modelle.

## NLP Subsystem (`src/main/java/org/example/service/nlp`)
- `DuuiNlpEngine.java`: HTTP-Client fuer externes DUUI-NLP und Mapping der Antwort ins Speech-Modell.
- `DuuiComposerNlpEngine.java`: DUUI-Composer-basierte Verarbeitung mit den Professor-Komponenten.
- `DuuiComposerPipeline.java`: baut die DUUI-Composer-Pipeline und harmonisiert die Component-Parameter.
- `NlpEngine.java`: Engine-Interface.
- `NlpEngineSelector.java`: waehlt aktive Engine aus Config/Verfuegbarkeit.
- `TextNormalizationUtil.java`: repariert Mojibake/Whitespace vor NLP und Timing.
- `UimaCasSerializer.java`: erzeugt kompakten UIMA-aehnlichen Snapshot in `speech.nlp`.
- `VideoSentenceTimestampService.java`: reichert Satzzeitempel `t0/t1` aus NLP-Segmenten oder lokaler Whisper-Ausgabe an.

## TeX Export (`src/main/java/org/example/service/tex`)
- `TeXEscaper.java`: escaped Sonderzeichen fuer LaTeX.
- `TeXPdfCompiler.java`: kompiliert TeX zu PDF (wenn `pdflatex` vorhanden).
- `TeXSpeechExporter.java`: baut TeX-Dokumente aus Speech-Daten + Templates.

## Utility (`src/main/java/org/example/util`)
- `DocumentMapper.java`: Mapping-Helfer zwischen BSON/JSON/Model.
- `VideoPathConfig.java`: zentrale Aufloesung lokaler Video-Root-Pfade + filename/videoid-Match.

## Frontend JS (`src/main/resources/public/js`)
- `analytics.js`: lädt/filtert Daten und rendert D3-Visualisierungen.
- `export.js`: UI-Logik fuer TeX/PDF-Export und Template-Editor.
- `index.js`: Dashboardaktionen (Import, NLP-Run, NLP-XMI-Import, Statusanzeige).
- `protocol-detail.js`: Detailseite fuer Protokoll inkl. Speech/Video-Listen.
- `protocols.js`: Protokolluebersicht (Listen/Filter/Reload).
- `speech-detail.js`: Speech-Detailinteraktion (NLP-Run, Videoimport, Sync-Hinweise, Annotation-Overlay).
- `speeches.js`: Speechliste mit Filtern, Navigation und Kurzmetadaten.

## Frontend Templates (`src/main/resources/templates`)
- `analytics.ftl`: Analytics-Seitenlayout.
- `export.ftl`: Export-Seitenlayout inkl. Template-Editor.
- `index.ftl`: Home/Dashboard.
- `protocol-detail.ftl`: Protokoll-Detailseite.
- `protocols.ftl`: Protokoll-Liste.
- `speech-detail.ftl`: Speech-Detailseite.
- `speeches.ftl`: Speech-Liste.

## Hinweis zu Alt-/Binärdateien
- `src/main/java/org/example/service/BundestagProtocolDownloader.class` ist Build-Artefakt (keine Quell-Logik).
