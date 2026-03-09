/* Developer guide: Speech browser UI with filtering, pagination-like limits, and quick navigation links. */
async function fetchJson(url) {
  const res = await fetch(url);
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  return data;
}

function escapeHtml(value) {
  return String(value || "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function setStatus(message, type = "") {
  const node = document.getElementById("speeches-status");
  if (!node) return;
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function setBusy(isBusy) {
  ["load-speeches", "search-speeches", "clear-filters"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.disabled = isBusy;
  });
}

function params() {
  const p = new URLSearchParams();
  const protocolId = document.getElementById("protocolId").value.trim();
  const protocolIds = document.getElementById("protocolIds").value.trim();
  const speakerId = document.getElementById("speakerId").value.trim();
  const faction = document.getElementById("faction").value.trim();
  const topic = document.getElementById("topic").value.trim();
  const matchMode = document.getElementById("matchMode").value;
  const from = document.getElementById("from").value.trim();
  const to = document.getElementById("to").value.trim();
  const limit = document.getElementById("limit").value || "200";

  if (protocolId) p.set("protocolId", protocolId);
  if (protocolIds) p.set("protocolIds", protocolIds);
  if (speakerId) p.set("speakerId", speakerId);
  if (faction) p.set("faction", faction);
  if (topic) p.set("topic", topic);
  if (matchMode) p.set("matchMode", matchMode);
  if (from) p.set("from", from);
  if (to) p.set("to", to);
  p.set("limit", limit);
  return p.toString();
}

function renderSpeeches(speeches) {
  return renderSpeechesWithVideos(speeches, []);
}

function renderSpeechesWithVideos(speeches, videos) {
  const list = document.getElementById("speech-list");
  list.innerHTML = "";
  const videoBySpeechId = new Map((videos || []).map(video => [video.speechId, video]));

  if (!Array.isArray(speeches) || speeches.length === 0) {
    const empty = document.createElement("p");
    empty.className = "meta";
    empty.textContent = "No speeches found for the current filters.";
    list.appendChild(empty);
    return;
  }

  for (const s of speeches) {
    const div = document.createElement("div");
    div.className = "item";
    const preview = escapeHtml((s.text || "").slice(0, 240));
    const speaker = s.speaker ? `${s.speaker.firstName || ""} ${s.speaker.lastName || ""}`.trim() : "Unknown";
    const faction = s.speaker?.parliamentaryGroup?.shortName || "";
    const id = escapeHtml(s.id || "");
    const protocolId = escapeHtml(s.protocolId || "");
    const video = videoBySpeechId.get(s.id);
    const videoHref = escapeHtml(video?.streamUrl || video?.sourceUrl || "");
    div.innerHTML = `
      <p class="meta">${id} | ${protocolId} | TOP ${escapeHtml(s.agendaItem || 0)} | speaker: ${escapeHtml(speaker)}${faction ? ` (${escapeHtml(faction)})` : ""}</p>
      <p class="meta">Video ${video ? "available" : "none"} | NLP ${s.nlpProcessed ? "available" : "pending"}</p>
      <p>${preview}${(s.text || "").length > 240 ? "..." : ""}</p>
      <p><a href="/speech/${encodeURIComponent(s.id)}">Open Detail</a>${video ? ` | <a href="${videoHref}" target="_blank" rel="noopener">Video</a>` : ""} | <a href="/analytics?speechId=${encodeURIComponent(s.id)}">Analyze</a></p>
    `;
    list.appendChild(div);
  }
}

async function loadSpeeches() {
  setBusy(true);
  setStatus("Loading speeches...");
  try {
    const speeches = await fetchJson(`/api/speeches?${params()}`);
    const speechIds = Array.isArray(speeches) ? speeches.map(speech => speech.id).filter(Boolean) : [];
    const videos = speechIds.length
      ? await fetchJson(`/api/videos?speechIds=${encodeURIComponent(speechIds.join(","))}&limit=${encodeURIComponent(speechIds.length)}`)
      : [];
    renderSpeechesWithVideos(speeches, videos);
    setStatus(`Loaded ${Array.isArray(speeches) ? speeches.length : 0} speeches.`, "status-ok");
  } catch (err) {
    setStatus(`Failed to load speeches: ${err.message}`, "status-error");
    throw err;
  } finally {
    setBusy(false);
  }
}

async function searchSpeeches() {
  const q = document.getElementById("searchQ").value.trim();
  const limit = document.getElementById("limit").value || "50";
  if (!q) {
    await loadSpeeches();
    return;
  }

  setBusy(true);
  setStatus(`Searching for "${q}"...`);
  try {
    const speeches = await fetchJson(`/api/speeches/search?q=${encodeURIComponent(q)}&limit=${encodeURIComponent(limit)}`);
    const speechIds = Array.isArray(speeches) ? speeches.map(speech => speech.id).filter(Boolean) : [];
    const videos = speechIds.length
      ? await fetchJson(`/api/videos?speechIds=${encodeURIComponent(speechIds.join(","))}&limit=${encodeURIComponent(speechIds.length)}`)
      : [];
    renderSpeechesWithVideos(speeches, videos);
    setStatus(`Search returned ${Array.isArray(speeches) ? speeches.length : 0} speeches.`, "status-ok");
  } catch (err) {
    setStatus(`Search failed: ${err.message}`, "status-error");
    throw err;
  } finally {
    setBusy(false);
  }
}

function clearFilters() {
  ["protocolId", "protocolIds", "speakerId", "faction", "topic", "from", "to", "searchQ"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.value = "";
  });
  document.getElementById("matchMode").value = "and";
  document.getElementById("limit").value = "50";
}

document.getElementById("load-speeches").addEventListener("click", () => {
  loadSpeeches().catch(() => {
    // Message already shown in status line.
  });
});

document.getElementById("search-speeches").addEventListener("click", () => {
  searchSpeeches().catch(() => {
    // Message already shown in status line.
  });
});

document.getElementById("clear-filters").addEventListener("click", () => {
  clearFilters();
  loadSpeeches().catch(() => {
    // Message already shown in status line.
  });
});

document.getElementById("searchQ").addEventListener("keydown", event => {
  if (event.key === "Enter") {
    event.preventDefault();
    searchSpeeches().catch(() => {
      // Message already shown in status line.
    });
  }
});

loadSpeeches().catch(() => {
  // Message already shown in status line.
});
