# Project Status Snapshot

## Implemented
- Java 21 Maven backend with Javalin and MongoDB
- Automated protocol import pipeline (preview, scoped import, single import)
- XML parsing for sessions, speeches, comments, deputies, speech video metadata
- Media enrichment/downloading scaffolding (deputy images, speech videos)
- REST API with Swagger UI and OpenAPI YAML
- Validation for API parameters and import schema checks
- MongoDB index initialization
- FreeMarker frontend pages:
  - Home, Protocols, Speeches, Speech Detail, Analytics, Export
- D3 analytics scaffolding for required chart types
- Export APIs:
  - TeX export
  - PDF export via pdflatex (if available)
  - Extended export filtering (`protocolIds`, `matchMode`, time range) and grouping (`protocol`, `speaker`, `faction`, `topic`, `none`)
- DB-backed export template management API + web editor
  - Added granular template snippets (`speech-entry`, `comment-entry`) in addition to section/header/footer
- NLP engine abstraction with configurable mode (`local`, `duui`, `auto`) and fallback behavior
- Lightweight in-app NLP fallback pipeline (`/api/nlp/run`) for chart testability
- UIMA-like NLP serialization payload (`uimaTypeSystem`, `uimaCas`) persisted in speech NLP metadata
- Speech detail enhancements:
  - coreference hover highlighting
  - comment author role badges (speaker/faction)
  - attribution provenance display for NLP-resolved comment metadata
- Frontend UX refinement pass:
  - inline status/error feedback on speeches/analytics/export pages
  - clear-filters action in speeches view
  - analytics time-range filter wiring (`from`/`to`)
- Planning artifacts completed in editable form:
  - Gantt, class/package/use-case diagrams, UI mockup
- Initial JUnit tests for parser/export/NLP service logic
- Full Maven unit test run completed successfully in local environment (`mvn -Dmaven.repo.local=.m2repo test`: 9 tests, 0 failures, 0 errors, 0 skipped on 2026-02-22)
- Final report and slide outline upgraded from skeleton to content-ready drafts
- Expanded JavaDoc pass completed across public model/api/service/config/db classes (2026-02-21)

## Partially Implemented / In Progress
- DUUI integration is endpoint-ready/configurable, but full assignment DUUI/UIMA pipeline contract and credentials wiring still needs finalization.
- Video workflow supports assignment-scoped speech clips via local `.local-data/bundestag-videos/` matching by Bundestag `videoid`; direct in-app playback is prioritized over embed fallbacks.
- LaTeX export architecture and grouping/filtering are extended, but assignment-level advanced layout/graphics (e.g. full TikZ options) still needs extension.
- Planning artifacts are in place in `docs/planning`; only team-specific contributor naming and final review remain.

## Not Yet Implemented
- Full DUUI processing pipeline for all required components (spaCy, ParlBERT-v2, GerVader, Coref, WhisperX) with production-ready credentials/runtime
- Full required frontend interaction richness tied to NLP (all toggles/behaviors)
- Final team-name mapping and submission sign-off for report/slide artifacts

## Operational Caveat
- DUUI/UIMA dependencies are heavy; clean environments may still require a long initial dependency download before first successful build/test.

<!-- [ITER2_EXPERIMENT_START] -->
Temporary experiment note for retrospective iteration trail.
Will be removed/refined in iter3.
<!-- [ITER2_EXPERIMENT_END] -->
