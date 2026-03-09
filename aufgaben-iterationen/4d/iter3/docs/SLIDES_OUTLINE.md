# Final Presentation Outline

## Slide 1 - Title
- Multimodal Parliament Explorer
- Team members (replace placeholders)
- Course context: PPR Assignment 5

## Slide 2 - Problem and Goal
- Challenge: parliamentary data is large, heterogeneous, and hard to compare quickly
- Goal: one workflow for import, NLP enrichment, interactive exploration, and export
- Scope: usable developer and evaluator setup with reproducible Maven runbook

## Slide 3 - System Architecture
- Backend: Javalin REST + service layer
- Persistence: MongoDB collections for protocol/session/speech/deputy/video/template
- Frontend: FreeMarker + vanilla JS + D3
- NLP: mode-based engine (`local`, `duui`, `auto`)
- Export: TeX generation + optional PDF compiler

## Slide 4 - Data Import Workflow
- Source discovery from Bundestag XML links
- XML parsing into domain model
- Upsert into MongoDB with identifiers and cross-references
- API exposure for protocols/sessions/speeches/deputies/videos

## Slide 5 - NLP Workflow
- Local heuristic pipeline available without external infrastructure
- DUUI-ready integration path with fallback handling
- Stored outputs: topics, entities, POS, sentence sentiment, coref, comment attribution
- Added UIMA-like CAS payload in speech NLP metadata

## Slide 6 - Frontend Features
- Speech list filters: protocol/speaker/faction/topic/range/match mode
- Speech detail toggles: comments/entities/coref/sentiment overlays
- Coreference hover highlighting and attribution provenance display
- Analytics charts: topic radar, POS sunburst, sentiment line, entity bar

## Slide 7 - Export and Templates
- TeX export with filtering and grouping (`protocol|speaker|faction|topic|none`)
- Optional TikZ stats block in TeX/PDF flow
- PDF endpoint with graceful failure when `pdflatex` missing
- Template API + web editor for reusable layout snippets

## Slide 8 - Demo Story
1. `GET /api/import/preview`
2. `POST /api/import/run`
3. Browse `/speeches` with combined filters
4. Run NLP (`POST /api/nlp/run?limit=...`)
5. Open `/analytics` and compare charts
6. Generate TeX/PDF and edit templates on `/export`

## Slide 9 - Testing and Quality
- Unit test scope: parser, NLP pipeline behavior, UIMA serialization, TeX exporter
- Verified test runs (February 21 and February 22, 2026): 9 tests, 0 failures, 0 errors
- Added status/error UX feedback lines on speeches/analytics/export pages
- Remaining quality work: DUUI production validation and broader integration testing

## Slide 10 - Status and Next Steps
Completed:
- End-to-end import -> NLP -> analytics -> export flow
- Local runnable mode with reproducible Maven command set

Open items:
- Production DUUI chain validation with credentials/runtime
- Final team contribution mapping and final proofreading
- Submission sign-off and final demo evidence packaging
