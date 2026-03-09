/* Developer guide: Protocol list UI including loading, filtering, and navigation behavior. */
async function fetchJson(url) {
  const res = await fetch(url);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

async function loadProtocols() {
  const limit = document.getElementById("limit").value || "200";
  const protocols = await fetchJson(`/api/protocols?limit=${encodeURIComponent(limit)}`);
  const tbody = document.querySelector("#protocol-table tbody");
  tbody.innerHTML = "";

  for (const p of protocols) {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${p.id}</td>
      <td>${p.legislativePeriod ?? ""}</td>
      <td>${p.sessionNumber ?? ""}</td>
      <td>${p.importedAt ?? ""}</td>
      <td>
        <a href="/protocol/${encodeURIComponent(p.id)}">Detail</a> |
        <a href="/speeches?protocolId=${encodeURIComponent(p.id)}">Speeches</a> |
        <a href="/analytics?protocolId=${encodeURIComponent(p.id)}">Analytics</a> |
        <a href="/export?protocolId=${encodeURIComponent(p.id)}">Export</a>
      </td>
    `;
    tbody.appendChild(tr);
  }
}

document.getElementById("load-protocols").addEventListener("click", () => {
  loadProtocols().catch(err => alert(err.message));
});

loadProtocols().catch(err => alert(err.message));
