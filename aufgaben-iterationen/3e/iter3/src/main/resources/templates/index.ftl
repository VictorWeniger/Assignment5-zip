<!-- Developer guide: Home dashboard layout for import and NLP operations. -->
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
    <h2>System Stats</h2>
    <div id="stats-grid" class="stats-grid"></div>
  </section>
  <section class="card">
    <h2>Quick Actions</h2>
    <div id="import-status" class="import-status idle">No protocol import running.</div>
    <p class="meta-note">Protocol import loads speeches, deputies, sessions, and metadata from the Bundestag source into the database.</p>
    <div class="actions">
      <button id="preview-import">Preview Import (period 20, limit 3)</button>
      <button id="run-import-3">Run Import (period 20, 3)</button>
      <button id="run-import-10">Run Import (period 20, 10)</button>
      <button id="run-import-all">Run Import (period 20, all)</button>
    </div>

    <hr/>

    <h3>DUUI NLP Runs</h3>
    <p class="meta-note">These buttons run the DUUI pipeline on speeches from the database.</p>
    <div id="duui-run-status" class="import-status idle">No DUUI NLP batch running.</div>
    <div class="row">
      <button id="run-nlp-3">Run DUUI NLP for 3 speeches</button>
    </div>
    <div class="row">
      <button id="run-nlp-300">Run DUUI NLP for 300 speeches</button>
    </div>
    <div class="row">
      <button id="run-nlp-all">Run DUUI NLP for all pending speeches</button>
    </div>

    <hr/>

    <h3>Professor XMI Import</h3>
    <p class="meta-note">This button does not run DUUI. It imports already prepared professor-provided XMI/UIMA annotations from local files.</p>
    <div class="row">
      <button id="import-nlp-file">Import Professor NLP XMI Files</button>
    </div>
    <div id="nlp-import-status" class="import-status idle">No professor NLP XMI import running.</div>
    <pre id="quick-output" class="output"></pre>
  </section>
  <section class="card">
    <h2>Video Candidate</h2>
    <p class="meta-note">Suggested protocol agenda items with multiple speeches from the importd data.</p>
    <div class="row wrap">
      <input id="video-protocol-id" placeholder="Protocol ID (e.g. 20-131)"/>
      <input id="video-agenda-item" placeholder="Agenda item (e.g. 7)"/>
    </div>
    <div class="row wrap">
      <input id="video-session-url" value="https://www.bundestag.de/mediathek/video?videoid=7649389" placeholder="Bundestag mediathek session URL"/>
      <input id="video-max-speeches" value="5" placeholder="Max speeches"/>
      <button id="auto-import-agenda-videos">Auto Import Agenda Videos</button>
      <button id="import-agenda-videos">Import Agenda Videos</button>
    </div>
    <div class="row wrap">
      <button id="refresh-video-candidates">Find Candidate</button>
    </div>
    <div id="video-candidate-status" class="import-status idle">No video candidate loaded.</div>
    <div id="video-candidate-list" class="list"></div>
  </section>
</main>
<script src="/js/index.js"></script>
</body>
</html>
