<!-- Developer guide: Export page layout for TeX/PDF controls and template editor sections. -->
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
    <div class="row wrap">
      <label for="title">Title</label>
      <input id="title" value="Parlamentsreden Export"/>
      <label for="protocolId">Protocol ID</label>
      <input id="protocolId" value="${protocolId!}" placeholder="optional"/>
      <label for="protocolIds">Protocol IDs (CSV)</label>
      <input id="protocolIds" placeholder="20-42,20-43"/>
      <label for="speakerId">Speaker ID</label>
      <input id="speakerId" placeholder="optional"/>
      <label for="faction">Faction</label>
      <input id="faction" placeholder="optional"/>
      <label for="topic">Topic</label>
      <input id="topic" placeholder="optional"/>
      <label for="matchMode">Match Mode</label>
      <select id="matchMode">
        <option value="and" selected>AND</option>
        <option value="or">OR</option>
      </select>
      <label for="groupBy">Group By</label>
      <select id="groupBy">
        <option value="protocol" selected>Protocol</option>
        <option value="speaker">Speaker</option>
        <option value="faction">Faction</option>
        <option value="topic">Topic</option>
        <option value="none">None</option>
      </select>
      <label for="from">From (ISO)</label>
      <input id="from" placeholder="2026-01-01T00:00:00Z"/>
      <label for="to">To (ISO)</label>
      <input id="to" placeholder="2026-12-31T23:59:59Z"/>
      <label for="limit">Limit</label>
      <input id="limit" type="number" value="200" min="1" max="1000"/>
      <label><input id="includeTikz" type="checkbox"/> Include TikZ NLP stats</label>
      <button id="generate-tex">Generate TeX</button>
      <button id="open-pdf">Open PDF</button>
    </div>
    <p id="export-status" class="meta-note" aria-live="polite"></p>
    <p class="meta-note">This page currently generates LaTeX source from imported speeches via <code>/api/export/tex</code>.</p>
    <p class="meta-note">Template placeholders: <code>${r"${title}"}</code>, <code>${r"${groupType}"}</code>, <code>${r"${groupLabel}"}</code>, <code>${r"${speechBlock}"}</code>.</p>
    <p class="meta-note">Speech entry placeholders include: <code>${r"${speakerName}"}</code>, <code>${r"${factionSuffix}"}</code>, <code>${r"${speechText}"}</code>, <code>${r"${nlpStats}"}</code>.</p>
  </section>

  <section class="card">
    <h2>TeX Preview</h2>
    <pre id="tex-output" class="output"></pre>
  </section>

  <section class="card">
    <h2>Template Editor</h2>
    <div class="row wrap">
      <label for="templateId">Template ID</label>
      <input id="templateId" value="speech-section"/>
      <label for="templateName">Template Name</label>
      <input id="templateName" placeholder="optional"/>
      <button id="seed-templates">Seed Defaults</button>
      <button id="load-template">Load</button>
      <button id="save-template">Save</button>
    </div>
    <textarea id="templateContent" class="template-area" placeholder="Template content"></textarea>
  </section>
</main>
<script src="/js/export.js"></script>
</body>
</html>
