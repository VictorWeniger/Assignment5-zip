# Smoke Test Runbook

Date: 2026-02-22

## Purpose
Quick end-to-end verification path for the demo workflow:
import -> speeches -> NLP -> analytics -> export.

## Preconditions
- MongoDB is running.
- Backend started with:
  - `mvn -Dmaven.repo.local=.m2repo exec:java`
  - for DUUI on Java 21, JVM options:
    `--add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED`
- Base URL: `http://localhost:7070`

## Step 1: Import Preview
```bash
curl -s "http://localhost:7070/api/import/preview?period=20&limit=2"
```
Expected:
- JSON array response with candidate rows.

## Step 2: Run Import
```bash
curl -s -X POST "http://localhost:7070/api/import/run?period=20&limit=2"
```
Expected:
- JSON summary with non-negative counters.

## Step 3: Query Speeches
```bash
curl -s "http://localhost:7070/api/speeches?limit=5"
```
Expected:
- JSON array of speech objects.

## Step 4: Trigger NLP Batch
```bash
curl -s -X POST "http://localhost:7070/api/nlp/run?limit=5&force=true"
```
Expected:
- JSON summary with processed/skipped fields.

## Step 5: Verify NLP Stats
```bash
curl -s "http://localhost:7070/api/nlp/stats"
```
Expected:
- JSON object with `totalSpeeches`, `nlpProcessedSpeeches`, `nlpPendingSpeeches`.

## Step 6: Analytics Topic Aggregation
```bash
curl -s "http://localhost:7070/api/topics?limit=10"
```
Expected:
- JSON array (possibly empty before enough NLP data exists).

## Step 7: Export TeX
```bash
curl -s "http://localhost:7070/api/export/tex?limit=10&groupBy=protocol" | head -n 20
```
Expected:
- TeX document header output.

## Step 8: Template API Check
```bash
curl -s -X POST "http://localhost:7070/api/templates/seed"
curl -s "http://localhost:7070/api/templates/speech-section"
```
Expected:
- Seed call returns `{"ok": true}`.
- Template lookup returns template JSON.

## Optional PDF Check
```bash
curl -i "http://localhost:7070/api/export/pdf?limit=5"
```
Expected:
- `200` with PDF stream when `pdflatex` is installed.
- `501` with JSON error when unavailable.
