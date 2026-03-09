# Multimodal Parliament Explorer

> Sync note (2026-03-09): This iteration was refreshed with the current Assignment5 DUUI-fixed state for task 3.1(e).

Backend core scaffold for Assignment 5.

## Requirements
- Java 21
- Maven 3.9+
- MongoDB

## Run
```bash
mvn -Dmaven.repo.local=.m2repo compile
mvn -Dmaven.repo.local=.m2repo test
```
At startup, the app creates MongoDB indexes for protocol/session/speech/deputy/video collections.
For DUUI-based NLP on Java 21, start the backend with VM options:
`--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED`

## Team Run Config (IntelliJ)
- Shared run config is committed at `.run/Assignment5 Backend.run.xml`.
- In IntelliJ, open the project root and select run config `Assignment5 Backend`.
- This config fixes `WORKING_DIRECTORY=$PROJECT_DIR$` so `PPR_2025_G_11_03.txt` is found reliably.
- This config must use these VM options for DUUI on Java 21:
  `--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED`
- If remote MongoDB is not reachable, check VPN/network access to `ppr.lehre.texttechnologylab.org:27020`.

## Tests
```bash
mvn -Dmaven.repo.local=.m2repo test
```

## API Docs
- Swagger UI: `http://localhost:7070/swagger`
- OpenAPI spec: `http://localhost:7070/swagger/openapi.yaml`
- Quick route list: `http://localhost:7070/api/docs`

## Frontend Pages
- Home dashboard: `http://localhost:7070/`
- Protocol browser: `http://localhost:7070/protocols`
- Speech browser: `http://localhost:7070/speeches`
- Speech detail: `http://localhost:7070/speech/{speechId}`
- Analytics (d3 scaffolding): `http://localhost:7070/analytics`
- Export (TeX preview): `http://localhost:7070/export`

## Project Docs
- User manual: `docs/USER_MANUAL.md`
- Status snapshot: `docs/PROJECT_STATUS.md`
- Planning artifacts: `docs/planning/README.md`
- Deliverable checklist: `docs/DELIVERABLE_CHECKLIST.md`
- Frontend rubric check: `docs/FRONTEND_RUBRIC_CHECK.md`
- Smoke test runbook: `docs/SMOKE_TEST_RUNBOOK.md`
- Final report draft: `docs/FINAL_REPORT.md`
- Slide outline: `docs/SLIDES_OUTLINE.md`

## NLP Import Format
- Supported input format is the professor-provided UIMA export only:
  - `.xmi.gz`
  - `.xmi`
- `POST /api/nlp/import` accepts either:
  - a single XMI file path
  - a directory path; the importer will scan it recursively for `.xmi` and `.xmi.gz`
- `TypeSystem.xml.gz` may be placed next to the XMI files. It is recorded as source metadata, but is not imported as a speech file.

## Local Video Clips
- For the assignment video subset, place downloaded Bundestag speech clips under:
  - `.local-data/bundestag-videos/`
- The app scans this folder recursively and auto-matches files by Bundestag `videoid`.
- Recommended naming:
  - `7649392_h264_512_288_514kb_baseline_de_514.mp4`
  - or any filename starting with the Bundestag `videoid`
- Do not place large video files under `src/main/resources`; Maven would try to copy them into `target/classes`.

## Video Playback Logic
- The assignment-scoped video workflow is intentionally limited to one selected agenda item with several speeches.
- Speeches outside the selected local video bundle will not show a playable in-app video. The UI reports this as an assignment-scope limitation, not as a hard application failure.
- Speech detail pages resolve videos in this order:
  1. local clip file under `.local-data/bundestag-videos/`
  2. direct stream URL if one could be extracted
  3. Bundestag embed fallback
- Local files are preferred because they give a normal in-app HTML5 player without Bundestag page chrome.
- If no local file is available, the app falls back to the proxied Bundestag embed and labels this clearly in the UI.
- To attach videos for a speech, open `/speech/{speechId}` and use:
  - `Import videos for this speech's agenda item`
- This resolves the speech to its agenda item, finds the matching Bundestag clip page for the speaker, and then prefers any local file with the same Bundestag `videoid`.

## Deputy Images
- Deputy images are already enriched during protocol import where a Bundestag image could be resolved.
- If media downloading is enabled, local image files are stored under the configured media directory and preferred automatically.
- Speech detail pages show the speaker portrait when available.

## Useful Test Calls
- `GET /api/import/preview?period=20&limit=2`
- `POST /api/import/run?period=20&limit=2`
- `POST /api/import/run/20-42?force=true`
- `POST /api/nlp/run?limit=300`
- `POST /api/nlp/run/{speechId}?force=true`
- `POST /api/nlp/import?path=/path/to/ID2010000100.xmi.gz&createMissing=true`
- `POST /api/nlp/import?path=/path/to/nlp-dokumente-abschlussprojekt&createMissing=true`
- `GET /api/nlp/stats`
- `GET /api/stats`
- `GET /api/protocols?limit=20` (summary only)
- `GET /api/protocols?limit=5&includeRaw=true` (includes raw XML)
- `GET /api/speeches?faction=SPD&limit=20`
- `GET /api/speeches?protocolIds=20-42,20-43&topic=Climate&matchMode=or&limit=50`
- `GET /api/speeches?from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z&limit=20`
- `GET /api/speeches/search?q=klimaschutz&limit=20`
- `GET /api/speeches/{id}/detail`
- `GET /api/topics?limit=100`
- `GET /api/export/tex?protocolId=20-42&limit=50`
- `GET /api/export/pdf?protocolId=20-42&limit=50`
- `GET /api/export/tex?protocolIds=20-42,20-43&matchMode=or&groupBy=faction&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z&limit=200`
- `GET /api/export/pdf?protocolId=20-42&groupBy=speaker&includeTikz=true&limit=100`
- `POST /api/templates/seed`
- `GET /api/templates`
- `PUT /api/templates/speech-section`
- `PUT /api/templates/speech-entry`

Validation notes:
- import `limit` max is `500`
- query `limit` max is `1000`

## Environment variables
- `MPE_PORT` (default: `7070`)
- `MPE_MONGO_URI` (optional; overrides local professor DB config file)
- `MPE_MONGO_DB` (optional; overrides local professor DB config file)
- `MPE_IMPORT_INTERVAL_MINUTES` (default: `120`)
- `MPE_DOWNLOAD_MEDIA` (default: `false`)
- `MPE_MEDIA_DIR` (default: `data/media`)
- `MPE_CREDENTIALS_FILE` (default: `.credentials/mpe.properties`)
- `MPE_NLP_ENGINE` (`duui` recommended; `local` is no longer supported in runtime)
- `MPE_DUUI_ENDPOINT` (default: empty)
- `MPE_DUUI_TOKEN` (default: empty)
- `MPE_DUUI_TIMEOUT_SECONDS` (default: `30`)
- `MPE_DUUI_MODE` (`remote`, `docker`, `mixed`; default: `remote`)
- `MPE_DUUI_WORKERS` (default: `1`)
- `MPE_DUUI_SPACY_TARGET` (required for DUUI pipeline)
- `MPE_DUUI_GERVADER_TARGET` (required for DUUI pipeline)
- `MPE_DUUI_PARLBERT_TOPIC_TARGET` (required for DUUI pipeline)
- `MPE_DUUI_SPACY_LANGUAGE` (default: `de`)
- `MPE_DUUI_SELECTION` (default: `sentences`)
- `MPE_DUUI_VIEW` (default: `speech`)
- `MPE_ENABLE_VIDEO_TIMESTAMPS` (default: `true`; enrich sentence `t0`/`t1` from local speech videos)
- `MPE_WHISPERX_CMD` (default: `whisper`; `whisperx` is also supported)
- `MPE_WHISPERX_MODEL` (optional; if empty WhisperX default model is used)
- `MPE_WHISPERX_TIMEOUT_SECONDS` (default: `1800`)
- `MPE_WHISPER_CACHE_DIR` (default: `.local-data/whisper-timestamps`)

## Credentials File
Copy `.credentials/mpe.properties.example` to `.credentials/mpe.properties` and fill values when provided:
```properties
mpe.nlp.engine=duui
mpe.duui.endpoint=
mpe.duui.token=
mpe.duui.timeoutSeconds=30
duui.mode=remote
duui.workers=1
duui.spacy.target=
duui.gervader.target=
duui.parlbert_topic.target=
duui.spacy.param.language=de
duui.param.selection=sentences
duui.param.view=speech
```
Environment variables override file values.

NLP runtime behavior:
- `POST /api/nlp/run...` executes DUUI NLP only.
- `POST /api/nlp/import...` imports professor-provided XMI/UIMA annotations only.
- No local heuristic NLP fallback is used anymore.

## MongoDB Startup Behavior
- Database config resolution order:
  1. `MPE_MONGO_URI` + `MPE_MONGO_DB`
  2. local `PPR_2025_G_11_03.txt`
  3. startup error with a clear message
- With your professor file present in the project root, the app should now target the remote course MongoDB automatically.
