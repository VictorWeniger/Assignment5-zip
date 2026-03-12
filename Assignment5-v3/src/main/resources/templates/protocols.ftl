<!-- Developer guide: Protocols overview page shell. -->
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
    <div class="row">
      <label for="limit">Limit</label>
      <input id="limit" type="number" value="200" min="1" max="1000"/>
      <button id="load-protocols">Load Protocols</button>
    </div>
    <table class="table" id="protocol-table">
      <thead>
      <tr>
        <th>ID</th>
        <th>Legislative Period</th>
        <th>Session</th>
        <th>Imported At</th>
        <th>Open</th>
      </tr>
      </thead>
      <tbody></tbody>
    </table>
  </section>
</main>
<script src="/js/protocols.js"></script>
</body>
</html>
