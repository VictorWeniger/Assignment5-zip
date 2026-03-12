<!-- Developer guide: Protocol detail page shell with controls and data containers. -->
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
    <p><strong>Protocol ID:</strong> <code id="protocol-id">${protocolId}</code></p>
    <div id="protocol-meta" class="subcard"></div>
    <p id="protocol-detail-status" class="meta-note" aria-live="polite"></p>
  </section>
  <section class="card">
    <div class="row wrap">
      <label for="protocol-detail-agenda">Agenda Item</label>
      <input id="protocol-detail-agenda" placeholder="Optional"/>
      <label for="protocol-detail-limit">Limit</label>
      <input id="protocol-detail-limit" type="number" value="200" min="1" max="1000"/>
      <button id="reload-protocol-detail">Reload</button>
    </div>
    <div id="protocol-speech-list" class="list"></div>
  </section>
</main>
<script>
  window.PROTOCOL_ID = "${protocolId}";
</script>
<script src="/js/protocol-detail.js"></script>
</body>
</html>
