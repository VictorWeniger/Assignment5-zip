<!-- Developer guide: Speech list page shell with filter controls. -->
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
      <label for="speakerId">Speaker ID</label>
      <input id="speakerId" placeholder="Optional"/>
      <label for="faction">Faction</label>
      <input id="faction" placeholder="Optional"/>
      <label for="topic">Topic</label>
      <input id="topic" placeholder="Optional"/>
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
      <button id="load-speeches">Load Speeches</button>
      <button id="clear-filters" type="button">Clear Filters</button>
    </div>
    <div class="row wrap">
      <label for="searchQ">Text Search</label>
      <input id="searchQ" placeholder="keyword"/>
      <button id="search-speeches">Search</button>
    </div>
    <p id="speeches-status" class="meta-note" aria-live="polite"></p>
    <div id="speech-list" class="list"></div>
  </section>
</main>
<script src="/js/speeches.js"></script>
</body>
</html>
