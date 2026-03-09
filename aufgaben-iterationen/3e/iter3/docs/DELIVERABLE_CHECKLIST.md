# Assignment 5 Deliverable Checklist

## 1. Core Application
- [x] Java 21 + Maven project with runnable backend
- [x] MongoDB persistence and index setup
- [x] Protocol import pipeline (preview, scoped import, single import)
- [x] REST API + OpenAPI + Swagger UI
- [x] FreeMarker frontend pages (home/protocols/speeches/detail/analytics/export)

## 2. NLP
- [x] DUUI-backed NLP processing path for batch and single-speech runs
- [x] Professor XMI/UIMA import path for preprocessed annotations
- [x] NLP metadata persisted per speech
- [x] UIMA-like CAS/type-system serialization fields stored with speech
- [ ] Broader DUUI validation (spaCy, ParlBERT-v2, GerVader, Coref, WhisperX) completed end-to-end with real runtime credentials

## 3. Export
- [x] TeX export API
- [x] PDF export API (via `pdflatex` if available)
- [x] Export filtering and grouping extensions
- [x] Template management + web editor
- [ ] Final advanced assignment-specific layout/polish (TikZ-heavy rendering variants)

## 4. Frontend
- [x] Speech list/search/filter interactions
- [x] Speech detail annotation toggles + coreference highlighting + comment role badges
- [x] Analytics view with chart scaffolding and filters
- [x] Final UX polish pass and acceptance rubric check documented in `docs/FRONTEND_RUBRIC_CHECK.md` (2026-02-21)

## 5. Planning + Documentation
- [x] Planning files in editable format (`docs/planning/*`)
- [x] User manual (`docs/USER_MANUAL.md`)
- [x] Project status snapshot (`docs/PROJECT_STATUS.md`)
- [x] Final report draft (`docs/FINAL_REPORT.md`)
- [x] Slide deck outline (`docs/SLIDES_OUTLINE.md`)
- [ ] Final team name mapping + final proofreading/sign-off

## 6. Quality
- [x] Initial unit tests for parser/NLP/export logic
- [x] Full test run in a stable environment with dependency cache complete (`mvn -Dmaven.repo.local=.m2repo test` on 2026-02-22)
- [x] JavaDoc completeness pass for public classes/methods completed on 2026-02-21 (including model/config/api/service/db layers)
