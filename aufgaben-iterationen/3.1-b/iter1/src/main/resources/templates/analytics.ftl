<!-- Developer guide: Analytics page layout and chart mount points. -->
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
      <label for="protocolId">Protocol ID</label>
      <input id="protocolId" value="${protocolId!}" placeholder="e.g. 20-42"/>
      <label for="protocolIds">Protocol IDs (CSV)</label>
      <input id="protocolIds" placeholder="20-42,20-43"/>
      <label for="speechId">Speech ID</label>
      <input id="speechId" value="${speechId!}" placeholder="optional single speech"/>
      <label for="faction">Faction</label>
      <input id="faction" placeholder="optional"/>
      <label for="topic">Topic</label>
      <select id="topic">
        <option value="">All</option>
      </select>
      <label for="matchMode">Match Mode</label>
      <select id="matchMode">
        <option value="and" selected>AND</option>
        <option value="or">OR</option>
      </select>
      <label for="from">From (ISO)</label>
      <input id="from" placeholder="2026-01-01T00:00:00Z"/>
      <label for="to">To (ISO)</label>
      <input id="to" placeholder="2026-12-31T23:59:59Z"/>
      <label for="limit">Limit</label>
      <input id="limit" type="number" value="200" min="1" max="1000"/>
      <button id="load-analytics">Load Analytics</button>
    </div>
    <p class="meta-note">Charts use available NLP fields in stored speech objects. If NLP is not imported yet, charts show placeholders.</p>
    <p id="analytics-status" class="meta-note" aria-live="polite"></p>
  </section>

  <section class="grid-2">
    <article class="card">
      <h2>Topics Radar</h2>
      <div id="topics-radar" class="viz"></div>
    </article>
    <article class="card">
      <h2>POS Sunburst</h2>
      <div id="pos-sunburst" class="viz"></div>
    </article>
    <article class="card">
      <h2>Sentiment Line</h2>
      <div id="sentiment-line" class="viz"></div>
    </article>
    <article class="card">
      <h2>Named Entities Bar</h2>
      <div id="entities-bar" class="viz"></div>
    </article>
  </section>
</main>
<script src="https://cdn.jsdelivr.net/npm/d3@7"></script>
<script src="/js/analytics.js"></script>
</body>
</html>
