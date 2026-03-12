# Code Walkthrough (Assignment5)

Dieses Dokument ist der schnelle Einstieg fuer Entwicklerinnen/Entwickler ohne Projektkontext.

## 1) High-Level Ablauf
1. App startet in [Application.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/Application.java).
2. Controller registrieren API + Frontend-Routen.
3. Import erzeugt Protokolle, Reden, Abgeordnete und Video-Referenzen.
4. NLP-Lauf annotiert Reden und kann Satz-Zeitstempel (`t0`/`t1`) aus Video-Transkripten anreichern.
5. Frontend liest diese Daten in Speech-Detail/Analytics.

## 2) Wichtige Module
- API:
  - [ImportController.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/api/ImportController.java)
  - [NlpController.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/api/NlpController.java)
  - [ProtocolController.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/api/ProtocolController.java)
- NLP Core:
  - [NlpProcessingService.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/NlpProcessingService.java)
  - [DuuiComposerNlpEngine.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/nlp/DuuiComposerNlpEngine.java)
  - [DuuiComposerPipeline.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/nlp/DuuiComposerPipeline.java)
  - [DuuiNlpEngine.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/nlp/DuuiNlpEngine.java)
  - [VideoSentenceTimestampService.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/nlp/VideoSentenceTimestampService.java)
  - [UimaCasSerializer.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/nlp/UimaCasSerializer.java)
- Import/Video:
  - [ProtocolImportService.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/ProtocolImportService.java)
  - [AgendaVideoImportService.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/AgendaVideoImportService.java)
  - [MediaAssetDownloadService.java](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/java/org/example/service/MediaAssetDownloadService.java)
- Frontend:
  - [speech-detail.js](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/resources/public/js/speech-detail.js)
  - [analytics.js](/Users/victorweniger/IdeaProjects_PPR/Assignment5/src/main/resources/public/js/analytics.js)

## 3) NLP und Timing im Detail
- Trigger:
  - `POST /api/nlp/run/{speechId}?force=true`
  - `POST /api/nlp/run?limit=...`
- Orchestrierung in `NlpProcessingService`:
  - Text wird vor NLP normalisiert (Mojibake-Reparatur).
  - Engine annotiert ueber DUUI.
  - Optional werden Satz-Timestamps angereichert.
  - UIMA-CAS Snapshot wird in `speech.nlp` abgelegt.
- Timing in `VideoSentenceTimestampService`:
  - Quelle 1: bereits vorhandene Segmente in `speech.nlp`.
  - Quelle 2: lokale Whisper/WhisperX-Ausgabe.
  - Segmenttexte werden auf `speech.text`-Offsets gemappt.
  - `t0/t1` pro Satz kommen aus Segment-Overlap (kein fuzzy window matching).
  - Status-Felder:
    - `syncMode`: `timed` oder `approx`
    - `timestampStatus`: `ok|partial|missing|already-present|no-sentences`
    - `timestampAssignedRows`, `timestampAlignmentScore`

## 4) Warum Ergebnisse manchmal schlecht sind
- Falsche/kaputte Textbasis (Encoding-Fehler, Sprecherwechsel im gleichen Block).
- DUUI-Lauf ist fehlgeschlagen oder alte Alt-Daten sind noch gespeichert.
- Video-Transkript und Rede-Text sind nicht 1:1 deckungsgleich.

## 5) Praktischer Debug-Workflow
1. Speech Detail laden: `GET /api/speeches/{id}/detail`.
2. Prüfen:
   - `speech.nlp.processingEngine`
   - `speech.nlp.syncMode`
   - `speech.nlp.timestampStatus`
   - `speech.nlp.timestampAssignedRows`
3. Falls schlecht:
   - mit `force=true` neu laufen lassen
   - lokale Video-Datei und Whisper-JSON im Cache pruefen
   - Text auf Mojibake/Strukturfehler pruefen

## 6) Frontend-Verhalten
- Speech-Detail zeigt jetzt klaren NLP-Run-Status:
  - running
  - success inkl. processed/skipped
  - error
- Video-Sync-Hinweis basiert auf `syncMode`/`timestampStatus`.
