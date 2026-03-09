# User Manual

## 1. Start the Application
1. Ensure MongoDB configuration is available:
   - preferred: keep `PPR_2025_G_11_03.txt` in the project root
   - optional override: set `MPE_MONGO_URI` and `MPE_MONGO_DB`
2. Set environment variables if needed:
   - `MPE_PORT` (default: `7070`)
   - `MPE_MONGO_URI` (optional override)
   - `MPE_MONGO_DB` (optional override)
   - `MPE_IMPORT_INTERVAL_MINUTES` (default: `120`)
   - `MPE_DOWNLOAD_MEDIA` (default: `false`)
   - `MPE_MEDIA_DIR` (default: `data/media`)
   - `MPE_BUNDLED_VIDEO_DIR` (default: `.local-data/bundestag-videos`)
   - `MPE_CREDENTIALS_FILE` (default: `.credentials/mpe.properties`)
   - `MPE_NLP_ENGINE` (`duui` recommended; `local` is no longer supported in runtime)
   - `MPE_DUUI_ENDPOINT` (optional)
   - `MPE_DUUI_TOKEN` (optional)
   - `MPE_DUUI_TIMEOUT_SECONDS` (default: `30`)
   - `MPE_DUUI_MODE` (`remote`, `docker`, `mixed`; default: `remote`)
   - `MPE_DUUI_WORKERS` (default: `1`)
   - `MPE_DUUI_SPACY_TARGET` (required for DUUI pipeline)
   - `MPE_DUUI_GERVADER_TARGET` (required for DUUI pipeline)
   - `MPE_DUUI_PARLBERT_TOPIC_TARGET` (required for DUUI pipeline)
   - `MPE_DUUI_SPACY_LANGUAGE` (default: `de`)
   - `MPE_DUUI_SELECTION` (default: `sentences`)
   - `MPE_DUUI_VIEW` (default: `speech`)
3. Run:
   - `mvn -Dmaven.repo.local=.m2repo compile`
   - `mvn -Dmaven.repo.local=.m2repo exec:java`
   - for DUUI on Java 21, start with VM options:
     `--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED`

Credentials file support:
- Copy `.credentials/mpe.properties.example` to `.credentials/mpe.properties`.
- Put DUUI values there when you receive credentials.
- Environment variables always override file values.
- In IntelliJ, the run configuration must include the same DUUI VM options on Java 21.

## 2. Main Pages
- Home: `http://localhost:7070/`
- Protocols: `http://localhost:7070/protocols`
- Speeches: `http://localhost:7070/speeches`
- Speech detail: `http://localhost:7070/speech/{speechId}`
- Analytics: `http://localhost:7070/analytics`
- Export: `http://localhost:7070/export`
- Swagger: `http://localhost:7070/swagger`

## 3. Import Workflow
1. Open Home page.
2. Run import preview.
3. Run import.
4. Verify counts in `GET /api/stats`.

For targeted import:
- `POST /api/import/run/{protocolId}?force=false`

## 4. Speech Exploration
- Use filters on `/speeches`:
  - protocol ID
  - multiple protocol IDs (CSV via `protocolIds`)
  - speaker ID
  - faction
  - topic
  - time range (`from` / `to`)
  - `matchMode=and|or` for combining filters
- Use text search field for fulltext lookup.
- Open speech detail to inspect metadata, comments, and linked video.
- In speech detail you can run NLP for the current speech and toggle:
  - inline comments
  - named entities
  - coreferences
  - sentence-level sentiment overlay
- If sentence timestamps are missing, the video/text sync falls back to an approximate alignment and the UI shows a hint.
- Comment metadata includes heuristic NLP attribution source/confidence when author fields are missing in raw XML.
- Local Bundestag speech clips:
  - Store downloaded clip files under `.local-data/bundestag-videos/`
  - Override the bundled video directory via `MPE_BUNDLED_VIDEO_DIR`
  - The app resolves them automatically by Bundestag `videoid`
  - On speech detail, locally resolved files are played as normal videos instead of embedding the Bundestag page

## 5. Analytics
- Open `/analytics`.
- Filter by protocol, speech, faction, topic.
- Optional: use `protocolIds` (CSV) and `matchMode=and|or`.
- Load charts:
  - Topics radar
  - POS sunburst
  - Sentiment line
  - Named-entities bar

Note: NLP charts depend on available NLP annotations in speech documents.
You can import pre-annotated files via:
- `POST /api/nlp/import?path=/path/to/file.xmi.gz&createMissing=false`
- `POST /api/nlp/import?path=/path/to/xmi-directory&createMissing=false`

NLP engine behavior:
- `POST /api/nlp/run...`: runs DUUI Java composer NLP only.
- `POST /api/nlp/import...`: imports professor-provided XMI/UIMA annotations only.
- No local heuristic NLP fallback is used anymore.

## 6. Export
### TeX
- Open `/export`
- Set filters and title
- Optional: `protocolIds` (CSV), `matchMode=and|or`, `groupBy` (`protocol|speaker|faction|topic|none`), `from`/`to`
- Optional: `includeTikz=true` to embed small NLP statistic bars into generated TeX/PDF.
- Click **Generate TeX**

### PDF
- Click **Open PDF**
- If LaTeX (`pdflatex`) is unavailable, API returns 501 with details.

### Template Editing
- In `/export`, use Template Editor.
- Seed defaults with **Seed Defaults**.
- Load/edit/save templates by ID:
  - `document-header`
  - `speech-section`
  - `speech-entry`
  - `comment-entry`
  - `document-footer`

NLP serialization note:
- After NLP processing/import, each speech stores:
  - `nlp.uimaTypeSystem`
  - `nlp.uimaCas`
