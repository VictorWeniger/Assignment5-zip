/* Developer guide: Protocol detail UI: fetches protocol/session/speeches/videos and renders combined view. */
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
  const node = document.getElementById("protocol-detail-status");
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function renderMeta(protocol, sessions, speeches) {
  const box = document.getElementById("protocol-meta");
  const session = Array.isArray(sessions) && sessions.length ? sessions[0] : null;
  box.innerHTML = `
    <p><strong>Legislative period:</strong> ${escapeHtml(protocol?.legislativePeriod ?? "")}</p>
    <p><strong>Session number:</strong> ${escapeHtml(protocol?.sessionNumber ?? "")}</p>
    <p><strong>Source:</strong> ${protocol?.sourceUrl ? `<a href="${escapeHtml(protocol.sourceUrl)}" target="_blank" rel="noopener">Open source XML</a>` : "n/a"}</p>
    <p><strong>Imported at:</strong> ${escapeHtml(protocol?.importedAt ?? "")}</p>
    <p><strong>Agenda entries:</strong> ${session?.agenda?.length ?? 0}</p>
    <p><strong>Loaded speeches:</strong> ${Array.isArray(speeches) ? speeches.length : 0}</p>
  `;
}

function renderSpeeches(speeches) {
  return renderSpeechesWithVideos(speeches, []);
}

function renderSpeechesWithVideos(speeches, videos) {
  const list = document.getElementById("protocol-speech-list");
  list.innerHTML = "";
  const videoBySpeechId = new Map((videos || []).map(video => [video.speechId, video]));

  if (!Array.isArray(speeches) || speeches.length === 0) {
    const empty = document.createElement("p");
    empty.className = "meta";
    empty.textContent = "No speeches found for this protocol and current filters.";
    list.appendChild(empty);
    return;
  }

  for (const speech of speeches) {
    const div = document.createElement("div");
    div.className = "item";
    const speaker = speech?.speaker ? `${speech.speaker.firstName || ""} ${speech.speaker.lastName || ""}`.trim() : "Unknown";
    const faction = speech?.speaker?.parliamentaryGroup?.shortName || "";
    const preview = escapeHtml((speech.text || "").slice(0, 320));
    const transcriptHint = speech.text ? `Transcript length ${speech.text.length}` : "No transcript text";
    const video = videoBySpeechId.get(speech.id);
    const videoHref = escapeHtml(video?.streamUrl || video?.sourceUrl || "");
    const videoMarkup = video
      ? `<a href="/speech/${encodeURIComponent(speech.id)}">Open Speech Detail</a> | <a href="${videoHref}" target="_blank" rel="noopener">Video</a> | <a href="/analytics?speechId=${encodeURIComponent(speech.id)}">Analyze</a>`
      : `<a href="/speech/${encodeURIComponent(speech.id)}">Open Speech Detail</a> | <a href="/analytics?speechId=${encodeURIComponent(speech.id)}">Analyze</a>`;
    div.innerHTML = `
      <p class="meta">${escapeHtml(speech.id || "")} | TOP ${escapeHtml(speech.agendaItem || 0)} | ${escapeHtml(speaker)}${faction ? ` (${escapeHtml(faction)})` : ""}</p>
      <p class="meta">${escapeHtml(transcriptHint)} | NLP ${speech.nlpProcessed ? "available" : "pending"} | Video ${video ? "available" : "none"}</p>
      <p>${preview}${(speech.text || "").length > 320 ? "..." : ""}</p>
      <p>${videoMarkup}</p>
    `;
    list.appendChild(div);
  }
}

async function loadProtocolDetail() {
  const protocolId = window.PROTOCOL_ID;
  const agendaItem = document.getElementById("protocol-detail-agenda").value.trim();
  const limit = document.getElementById("protocol-detail-limit").value || "200";

  setStatus("Loading protocol detail...");
  try {
    const [protocol, sessions, speeches] = await Promise.all([
      fetchJson(`/api/protocols/${encodeURIComponent(protocolId)}`),
      fetchJson(`/api/sessions?protocolId=${encodeURIComponent(protocolId)}`),
      fetchJson(`/api/speeches?protocolId=${encodeURIComponent(protocolId)}${agendaItem ? `&agendaItem=${encodeURIComponent(agendaItem)}` : ""}&limit=${encodeURIComponent(limit)}`)
    ]);
    const speechIds = Array.isArray(speeches) ? speeches.map(speech => speech.id).filter(Boolean) : [];
    const videos = speechIds.length
      ? await fetchJson(`/api/videos?speechIds=${encodeURIComponent(speechIds.join(","))}&limit=${encodeURIComponent(limit)}`)
      : [];
    renderMeta(protocol, sessions, speeches);
    renderSpeechesWithVideos(speeches, videos);
    setStatus(`Loaded protocol ${protocolId} with ${Array.isArray(speeches) ? speeches.length : 0} speeches.`, "status-ok");
  } catch (err) {
    setStatus(`Failed to load protocol detail: ${err.message}`, "status-error");
  }
}

document.getElementById("reload-protocol-detail").addEventListener("click", () => {
  loadProtocolDetail();
});

loadProtocolDetail();
