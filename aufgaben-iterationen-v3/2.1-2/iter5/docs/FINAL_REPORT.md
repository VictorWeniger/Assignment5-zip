# Multimodal Parliament Explorer - Final Report

## 1. Project Context and Goal
The Assignment 5 goal is a multimodal analytics platform for German Bundestag plenary protocols, combining ingestion, structuring, NLP enrichment, exploration, and export in one reproducible Java project.

Primary user groups:
- Students and instructors validating assignment requirements
- Developers extending import/NLP/export modules
- Analysts exploring speech content, comments, entities, sentiment, and topic signals

Target deliverables covered by this project:
- Java 21 + Maven backend
- MongoDB persistence model and indexes
- REST API with OpenAPI/Swagger exposure
- Browser UI for protocol and speech exploration
- NLP processing pipeline abstraction (local/DUUI modes)
- TeX/PDF export with filters, grouping, and templates
- Planning, user guide, and final documentation artifacts

## 2. Architecture
### 2.1 Backend
Technology stack:
- Javalin as HTTP framework
- Layered services for import, NLP, and export
- MongoDB collections for protocols, sessions, speeches, deputies, videos, templates

Key backend responsibilities:
- Protocol import orchestration and parsing
- Speech filtering/search endpoints for UI and export
- NLP execution and import integration
- TeX generation and optional PDF compilation

### 2.2 Frontend
Frontend is delivered with FreeMarker templates and vanilla JavaScript:
- Route pages: home, protocols, speeches, speech detail, analytics, export
- Filter forms for protocol/speaker/faction/topic/range constraints
- D3-based analytics renderers (radar, sunburst, line, bar)
- Export preview and template editor

### 2.3 External Integrations
- Bundestag XML sources for protocol ingestion
- DUUI endpoint integration points via configurable NLP mode
- Optional tooling: `pdflatex` for PDF generation from TeX

## 3. Data Model and Processing Flow
### 3.1 Import Pipeline
Flow:
1. Fetch XML links from Bundestag source pages.
2. Parse protocol id (`legislativePeriod-sessionNumber`) from URL.
3. Parse XML into protocol/session/speech/comment/deputy/video objects.
4. Upsert parsed objects into MongoDB collections.

Operational endpoints:
- `GET /api/import/preview`
- `POST /api/import/run`
- `POST /api/import/run/{protocolId}`

### 3.2 NLP Pipeline
NLP processing is DUUI-driven:
- `POST /api/nlp/run...`: runs the professor DUUI pipeline for batch or single-speech processing
- `POST /api/nlp/import...`: imports professor-provided XMI/UIMA annotations
- no local heuristic fallback is used in the runtime processing path anymore

Stored artifacts include:
- Topics
- Named entities
- POS distribution
- Sentence sentiments
- Coreference mention groups
- UIMA-like CAS payload (`nlp.uimaTypeSystem`, `nlp.uimaCas`)

NLP endpoints:
- `POST /api/nlp/run`
- `POST /api/nlp/run/{speechId}`
- `POST /api/nlp/import`
- `GET /api/nlp/stats`

### 3.3 Export Pipeline
Export flow:
1. Query speeches with filter/match parameters.
2. Apply optional time-range and grouping logic.
3. Render TeX using default or DB-backed templates.
4. Optionally compile TeX to PDF via `pdflatex`.

Export endpoints:
- `GET /api/export/tex`
- `GET /api/export/pdf`
- `GET/PUT/POST /api/templates*`

## 4. Implemented Features
Completed implementation areas:
- Import preview/scoped/single protocol import
- Protocol/session/speech/deputy/video read APIs
- Speech search and filter combinations including `protocolIds` and `matchMode`
- Speech detail view with annotation toggles
- Coreference hover highlighting and comment attribution metadata view
- Analytics dashboard with four chart types
- TeX/PDF export with grouping (`protocol|speaker|faction|topic|none`)
- Export template persistence with granular snippet templates
- DUUI-backed NLP processing and UIMA-like serialization persistence

## 5. Evaluation and Testing
### 5.1 Test Strategy
Unit tests cover parser and service logic in areas with highest transformation risk:
- Protocol id parsing
- TeX escaping
- TeX export rendering/grouping behavior
- DUUI-oriented NLP processing behavior
- CAS serialization and metadata persistence

### 5.2 Results
Executed command in project root:
```bash
mvn -Dmaven.repo.local=.m2repo test
```
Verified successful runs on February 21 and February 22, 2026:
- Tests run: 9
- Failures: 0
- Errors: 0
- Skipped: 0
- Build status: `BUILD SUCCESS`

### 5.3 Limitations
- Full DUUI validation across all required components and larger processing volumes is still ongoing.
- Advanced assignment-specific TeX layout variants (larger TikZ/report-grade formatting) are still limited.
- Team-name mapping and final submission sign-off are still pending.

## 6. Project Management
Planning artifacts were maintained in editable format:
- `docs/planning/gantt.mmd`
- `docs/planning/class-diagram.puml`
- `docs/planning/package-diagram.puml`
- `docs/planning/use-cases.puml`
- `docs/planning/mockup.md`

Execution approach:
- Implement thin end-to-end slices first (import -> browse -> NLP -> export)
- Add validation and query constraints for API robustness
- Backfill tests and docs during stabilization phase

## 7. Team Contributions
Replace this section with final named mapping before submission.

Suggested structure:
- Member A: import/parser/database integration
- Member B: NLP services and annotation UX
- Member C: export/template system and PDF handling
- Member D: frontend polish, analytics, and documentation

## 8. Known Gaps and Future Work
Priority backlog:
1. Additional DUUI regression validation with real infrastructure and credentials.
2. Additional frontend accessibility review in browser-based acceptance tests.
3. Broader automated tests (integration/API-level tests in addition to unit tests).
4. Final team-name mapping and sign-off details.

## 9. Conclusion
The project delivers a working multimodal parliament explorer with robust import, DUUI-backed NLP, query, and export capabilities in one Maven/Javalin/MongoDB stack. The remaining work is concentrated in broader DUUI validation, final documentation completeness, and final UX refinement.

## Appendix A: Runbook
```bash
mvn -Dmaven.repo.local=.m2repo compile
mvn -Dmaven.repo.local=.m2repo test
mvn -Dmaven.repo.local=.m2repo exec:java
```

## Appendix B: Important Endpoints
- `/api/import/*`
- `/api/nlp/*`
- `/api/speeches*`
- `/api/export/*`
- `/api/templates/*`
- `/api/stats`

## Appendix C: Smoke Test
- Runbook: `docs/SMOKE_TEST_RUNBOOK.md`
