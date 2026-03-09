<!-- Developer guide: Speech detail page shell with player, annotation toggles, and action buttons. -->
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>${title}</title>
  <link rel="stylesheet" href="/css/app.css"/>
</head>
<body>
<header class="topbar">
  <h1>${title}</h1>
  <nav>
    <a href="/">Home</a>
    <a href="/protocols">Protocols</a>
    <a href="/speeches">Speeches</a>
    <a href="/analytics">Analytics</a>
    <a href="/export">Export</a>
    <a href="/swagger">Swagger</a>
  </nav>
</header>
<main class="container">
  <section class="card">
    <p><strong>Speech ID:</strong> <code id="speech-id">${speechId}</code></p>
    <div id="speaker-box" class="subcard"></div>
    <div id="video-box" class="subcard"></div>
    <p id="sync-note" class="meta-note"></p>
    <div class="subcard">
      <div class="row wrap">
        <button id="import-speech-agenda-videos">Import videos for this speech's agenda item</button>
        <button id="reimport-speech-protocol" hidden>Reimport this speech's protocol</button>
      </div>
      <p id="speech-video-import-status" class="meta-note" aria-live="polite"></p>
    </div>
    <div class="row wrap">
      <button id="run-nlp-speech">Run NLP for this speech</button>
      <label><input type="checkbox" id="toggle-comments" checked/> Show inline comments</label>
      <label><input type="checkbox" id="toggle-entities" checked/> Show named entities</label>
      <label><input type="checkbox" id="toggle-sarcasm" checked/> Show sentence sarcasm</label>
      <label><input type="checkbox" id="toggle-sentiment" checked/> Show sentence sentiment</label>
      <label>Sarcasm threshold
        <input id="sarcasm-threshold" type="range" min="0" max="1" step="0.05" value="0.45"/>
        <span id="sarcasm-threshold-value">0.45</span>
      </label>
      <div id="sentiment-traffic" class="sentiment-traffic neutral" title="Sentence sentiment for current playback position">
        <span class="sentiment-dot sentiment-dot-positive" aria-label="positive"></span>
        <span class="sentiment-dot sentiment-dot-neutral" aria-label="neutral"></span>
        <span class="sentiment-dot sentiment-dot-negative" aria-label="negative"></span>
      </div>
      <span id="sentiment-current" class="meta-note"></span>
    </div>
    <p id="nlp-run-status" class="meta-note" aria-live="polite"></p>
    <article id="speech-text" class="speech-text"></article>
    <section class="subcard">
      <h3>Comments</h3>
      <div id="comment-list" class="list"></div>
    </section>
  </section>
</main>
<script>
  window.SPEECH_ID = "${speechId}";
</script>
<script src="/js/speech-detail.js"></script>
</body>
</html>
