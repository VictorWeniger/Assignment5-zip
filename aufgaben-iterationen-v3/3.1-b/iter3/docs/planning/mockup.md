# UI Mockup (Textual)

## Navigation
- Top bar links: `Home | Protocols | Speeches | Analytics | Export | Swagger`
- Shared filter style with compact row layout and mobile wrapping.

## Home
- Import controls:
  - Preview import candidates
  - Import latest protocol set
  - Import specific protocol
- Stats cards:
  - protocols, sessions, speeches, deputies, videos
  - NLP processed / pending

## Protocols
- Table:
  - Protocol ID, legislative period, session number, imported timestamp
- Quick actions:
  - open protocol details
  - open speeches filtered by protocol

## Speeches
- Filter row:
  - protocol ID
  - protocol IDs (CSV)
  - speaker ID
  - faction
  - topic
  - match mode (AND/OR)
  - from/to
  - limit
- Result cards:
  - metadata row + preview text
  - links: detail view, analytics view

## Speech Detail
- Speaker box:
  - name, faction, optional photo
- Video box:
  - linked source URL + embedded player
  - sentiment lamp synced to playback
- Toggles:
  - comments, entities, coreferences, sentence sentiment
- Text area:
  - inline annotation overlays
  - coreference hover highlights related mentions
- Comments panel:
  - badge for source role (`SPK` / `FRA`)
  - author + faction + offset

## Analytics
- Filters:
  - protocol ID / protocol IDs
  - optional single speech ID
  - faction, topic
  - match mode
  - limit
- Charts:
  - Topics Radar
  - POS Sunburst
  - Sentiment Line
  - Named Entities Horizontal Bar

## Export
- Filters:
  - protocol/protocol IDs, speaker, faction, topic, from/to
  - match mode
  - group by (protocol/speaker/faction/topic/none)
  - include TikZ
  - limit, title
- Actions:
  - Generate TeX
  - Open PDF inline
- Template editor:
  - load/seed/save
  - template IDs: `document-header`, `speech-section`, `speech-entry`, `comment-entry`, `document-footer`
