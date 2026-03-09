/* Developer guide: Speech detail UI for inline annotations, video sync, NLP run feedback, and video import actions. */
async function fetchJson(url, options = {}) {
  const method = String(options?.method || "GET").toUpperCase();
  const merged = method === "GET"
    ? { cache: "no-store", ...options }
    : options;
  const res = await fetch(url, merged);
  const raw = await res.text();
  let data = {};
  try {
    data = raw ? JSON.parse(raw) : {};
  } catch {
    data = { message: raw };
  }
  if (!res.ok) throw new Error(data.message || data.error || `HTTP ${res.status}`);
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

function collectAnnotations(speech, toggles) {
  const text = speech?.text || "";
  const annotations = [];

  if (toggles.sentiment) {
    const rows = speech?.nlp?.sentenceSentiments || [];
    rows.forEach((row, idx) => {
      if (!Number.isInteger(row.begin) || !Number.isInteger(row.end) || row.end <= row.begin) return;
      let cls = "speech-sent-neutral";
      const score = Number(row.score ?? 0);
      if (score > 0.15) cls = "speech-sent-positive";
      else if (score < -0.15) cls = "speech-sent-negative";
      annotations.push({
        begin: row.begin,
        end: row.end,
        priority: 1,
        cls,
        title: `Sentiment ${score.toFixed(2)}`,
        attrs: { "data-sent-idx": String(idx) }
      });
    });
  }

  if (toggles.entities) {
    const rows = speech?.nlp?.namedEntities || speech?.namedEntities || [];
    rows.forEach(row => {
      if (!Number.isInteger(row.begin) || !Number.isInteger(row.end) || row.end <= row.begin) return;
      annotations.push({
        begin: row.begin,
        end: row.end,
        priority: 2,
        cls: "speech-inline-entity",
        title: `Entity ${row.type || ""}`
      });
    });
  }

  if (toggles.sarcasm) {
    const rows = speech?.nlp?.sentenceSarcasm || speech?.sentenceSarcasm || [];
    const threshold = Number.isFinite(toggles.sarcasmThreshold) ? toggles.sarcasmThreshold : 0.45;
    rows.forEach(row => {
      if (!Number.isInteger(row.begin) || !Number.isInteger(row.end) || row.end <= row.begin) return;
      const score = Number(row.score ?? 0);
      if (score < threshold) return;
      annotations.push({
        begin: row.begin,
        end: row.end,
        priority: 3,
        cls: "speech-inline-sarcasm",
        title: `Sarcasm ${score.toFixed(2)}`
      });
    });
  }

  if (toggles.comments) {
    (speech?.comments || []).forEach(c => {
      const begin = Number(c.speechOffset);
      const t = c.text || "";
      if (!Number.isInteger(begin) || begin < 0 || !t) return;
      annotations.push({
        begin,
        end: begin + t.length,
        priority: 4,
        cls: "speech-inline-comment",
        title: "Comment"
      });
    });
  }

  return annotations
    .filter(a => a.begin >= 0 && a.end <= text.length && a.end > a.begin)
    .sort((a, b) => a.begin - b.begin || b.priority - a.priority);
}

function renderAnnotatedText(text, annotations) {
  if (!annotations.length) return escapeHtml(text);

  const starts = new Map();
  const ends = new Map();
  annotations.forEach((a, idx) => {
    if (!starts.has(a.begin)) starts.set(a.begin, []);
    if (!ends.has(a.end)) ends.set(a.end, []);
    starts.get(a.begin).push({ ...a, id: idx });
    ends.get(a.end).push({ ...a, id: idx });
  });

  const opened = [];
  let html = "";

  for (let i = 0; i <= text.length; i++) {
    const closeAt = ends.get(i) || [];
    if (closeAt.length) {
      closeAt.sort((x, y) => x.priority - y.priority).forEach(close => {
        const idx = opened.findIndex(o => o.id === close.id);
        if (idx >= 0) {
          // close nested tags from top until target and reopen others
          const toReopen = [];
          for (let j = opened.length - 1; j >= idx; j--) {
            html += "</span>";
            if (j !== idx) toReopen.push(opened[j]);
          }
          opened.splice(idx);
          toReopen.reverse().forEach(tag => {
            html += `<span class=\"${tag.cls}\" title=\"${escapeHtml(tag.title)}\">`;
            opened.push(tag);
          });
        }
      });
    }

    const openAt = starts.get(i) || [];
    if (openAt.length) {
      openAt.sort((x, y) => x.priority - y.priority).forEach(a => {
        const attrs = a.attrs
          ? Object.entries(a.attrs).map(([k, v]) => ` ${k}=\"${escapeHtml(v)}\"`).join("")
          : "";
        html += `<span class=\"${a.cls}\" title=\"${escapeHtml(a.title)}\"${attrs}>`;
        opened.push(a);
      });
    }

    if (i < text.length) {
      html += escapeHtml(text[i]);
    }
  }

  while (opened.length) {
    opened.pop();
    html += "</span>";
  }

  return html;
}

function renderCommentList(comments, speech = {}) {
  const list = document.getElementById("comment-list");
  list.innerHTML = "";

  if (!Array.isArray(comments) || comments.length === 0) {
    const p = document.createElement("p");
    p.className = "meta";
    p.textContent = "No comments available.";
    list.appendChild(p);
    return;
  }

  comments.forEach((c, idx) => {
    const div = document.createElement("div");
    div.className = "comment-item";
    const badge = c.authorFaction
      ? `<span class="comment-badge faction">FRA</span>`
      : `<span class="comment-badge speaker">SPK</span>`;
    const attributions = Array.isArray(speech?.nlp?.commentAttributions) ? speech.nlp.commentAttributions : [];
    const match = attributions.find(a => Number(a.speechOffset) === Number(c.speechOffset));
    const meta = `${idx + 1}. offset=${c.speechOffset ?? "n/a"} | ${c.authorName || "Unknown"} ${c.authorFaction ? `(${c.authorFaction})` : ""}${match?.source ? ` | source=${match.source}` : ""}${Number.isFinite(Number(match?.confidence)) ? ` | confidence=${Number(match.confidence).toFixed(2)}` : ""}`;
    div.innerHTML = `<div class=\"meta\">${badge} ${escapeHtml(meta)}</div><div>${escapeHtml(c.text || "")}</div>`;
    list.appendChild(div);
  });
}

function sentimentClass(score) {
  if (score > 0.15) return "positive";
  if (score < -0.15) return "negative";
  return "neutral";
}

function bindVideoSentimentLamp(videoEl, speech) {
  const traffic = document.getElementById("sentiment-traffic");
  const currentSentenceNode = document.getElementById("sentiment-current");
  if (!videoEl || !traffic) return;

  const rows = speech?.nlp?.sentenceSentiments || [];
  const spokenRows = Array.isArray(speech?.nlp?.spokenSentences) ? speech.nlp.spokenSentences : [];
  if (!rows.length) {
    traffic.className = "sentiment-traffic neutral";
    if (currentSentenceNode) {
      currentSentenceNode.textContent = "";
    }
    return;
  }

  const timed = rows
    .map((row, idx) => ({
      idx,
      t0: Number(row?.t0),
      t1: Number(row?.t1)
    }))
    .filter(r => Number.isFinite(r.t0))
    .sort((a, b) => a.t0 - b.t0);

  for (let i = 0; i < timed.length; i++) {
    if (!Number.isFinite(timed[i].t1) || timed[i].t1 < timed[i].t0) {
      timed[i].t1 = i + 1 < timed.length ? timed[i + 1].t0 : timed[i].t0 + 0.25;
    }
    if (timed[i].t1 < timed[i].t0) {
      timed[i].t1 = timed[i].t0;
    }
  }

  const timestampStatus = String(speech?.nlp?.timestampStatus || "").toLowerCase();
  const timedCoverage = timed.length / Math.max(1, rows.length);
  const hasRealTimestamps = timed.length > 0
    && (timestampStatus === "ok" || timestampStatus === "already-present" || timedCoverage >= 0.7);
  const spokenTimed = spokenRows
    .map((row, idx) => ({
      idx,
      sentence: String(row?.sentence || "").trim(),
      t0: Number(row?.t0 ?? row?.start),
      t1: Number(row?.t1 ?? row?.end)
    }))
    .filter(r => Number.isFinite(r.t0))
    .sort((a, b) => a.t0 - b.t0);
  for (let i = 0; i < spokenTimed.length; i++) {
    if (!Number.isFinite(spokenTimed[i].t1) || spokenTimed[i].t1 < spokenTimed[i].t0) {
      spokenTimed[i].t1 = i + 1 < spokenTimed.length ? spokenTimed[i + 1].t0 : spokenTimed[i].t0 + 0.25;
    }
    if (spokenTimed[i].t1 < spokenTimed[i].t0) {
      spokenTimed[i].t1 = spokenTimed[i].t0;
    }
  }
  const hasSpokenTimed = spokenTimed.length > 0;
  const spokenToSentenceIdx = buildSpokenToSentenceMap(spokenTimed, rows);
  let lastIdx = -1;
  let lastSpokenIdx = -1;

  function normSentence(value) {
    return String(value || "")
      .toLowerCase()
      .replace(/[^\p{L}\p{N}\s]+/gu, " ")
      .replace(/\s+/g, " ")
      .trim();
  }

  function overlapScore(a, b) {
    if (!a || !b) return 0;
    if (a === b) return 1;
    if (a.includes(b) || b.includes(a)) {
      const min = Math.min(a.length, b.length);
      const max = Math.max(a.length, b.length);
      return max > 0 ? min / max : 0;
    }
    const at = new Set(a.split(" ").filter(Boolean));
    const bt = new Set(b.split(" ").filter(Boolean));
    if (!at.size || !bt.size) return 0;
    let common = 0;
    at.forEach(t => {
      if (bt.has(t)) common++;
    });
    return common / Math.max(at.size, bt.size);
  }

  function buildSpokenToSentenceMap(spoken, sentenceRows) {
    const mapping = new Array(spoken.length).fill(-1);
    if (!spoken.length || !sentenceRows.length) return mapping;

    const normalizedRows = sentenceRows.map((row, idx) => ({
      idx,
      norm: normSentence(row?.sentence || "")
    }));

    let cursor = 0;
    for (let i = 0; i < spoken.length; i++) {
      const sNorm = normSentence(spoken[i]?.sentence || "");
      if (!sNorm) continue;

      let bestIdx = -1;
      let bestScore = 0;

      for (let j = Math.max(0, cursor - 2); j < normalizedRows.length; j++) {
        const score = overlapScore(sNorm, normalizedRows[j].norm);
        if (score > bestScore) {
          bestScore = score;
          bestIdx = normalizedRows[j].idx;
        }
        // exact hit -> stop early
        if (bestScore >= 0.999) break;
      }

      if (bestIdx >= 0 && bestScore >= 0.28) {
        mapping[i] = bestIdx;
        cursor = bestIdx;
      }
    }
    return mapping;
  }

  function findTimedIndex(t) {
    if (!timed.length) return -1;
    let lo = 0;
    let hi = timed.length - 1;
    let pos = -1;
    while (lo <= hi) {
      const mid = (lo + hi) >> 1;
      if (t >= timed[mid].t0) {
        pos = mid;
        lo = mid + 1;
      } else {
        hi = mid - 1;
      }
    }
    if (pos < 0) return timed[0].idx;
    const cur = timed[pos];
    if (t >= cur.t0 && t < cur.t1) return cur.idx;
    return cur.idx;
  }

  function approxIndexByTime(t) {
    const n = rows.length;
    if (!n) return -1;

    let d = Number(videoEl.duration);
    if (!Number.isFinite(d) || d <= 0) {
      try {
        if (videoEl.seekable && videoEl.seekable.length > 0) {
          d = videoEl.seekable.end(videoEl.seekable.length - 1);
        }
      } catch (_) {
        d = NaN;
      }
    }

    if (!Number.isFinite(d) || d <= 0) {
      return Math.max(0, Math.min(n - 1, Math.floor(t * 2)));
    }

    const slot = d / n;
    return Math.max(0, Math.min(n - 1, Math.floor(t / slot)));
  }

  function updateForTime(t) {
    if (!Number.isFinite(t) || t < 0) return;
    let idx = hasRealTimestamps ? findTimedIndex(t) : approxIndexByTime(t);
    if (idx < 0) idx = 0;

    const row = rows[idx] || {};
    const score = Number(row.score ?? 0);
    traffic.className = `sentiment-traffic ${sentimentClass(score)}`;
    if (hasSpokenTimed && currentSentenceNode) {
      let spokenPos = -1;
      let lo = 0;
      let hi = spokenTimed.length - 1;
      while (lo <= hi) {
        const mid = (lo + hi) >> 1;
        if (t >= spokenTimed[mid].t0) {
          spokenPos = mid;
          lo = mid + 1;
        } else {
          hi = mid - 1;
        }
      }
      if (spokenPos < 0) spokenPos = 0;
      if (spokenPos !== lastSpokenIdx) {
        lastSpokenIdx = spokenPos;
      }
      const spokenSentence = spokenTimed[spokenPos]?.sentence || "";
      currentSentenceNode.textContent = spokenSentence ? spokenSentence.slice(0, 180) : "";

      const mappedSentenceIdx = spokenToSentenceIdx[spokenPos];
      if (Number.isInteger(mappedSentenceIdx) && mappedSentenceIdx >= 0) {
        idx = mappedSentenceIdx;
      }
    } else if (currentSentenceNode) {
      const sentence = String(row.sentence || "").trim();
      currentSentenceNode.textContent = sentence ? sentence.slice(0, 140) : "";
    }
    if (idx !== lastIdx) {
      lastIdx = idx;
      highlightCurrentSentence(idx);
    }
  }

  videoEl.addEventListener("timeupdate", () => updateForTime(videoEl.currentTime));
  videoEl.addEventListener("seeked", () => updateForTime(videoEl.currentTime));
  videoEl.addEventListener("loadedmetadata", () => updateForTime(0));
}

function highlightCurrentSentence(idx) {
  document.querySelectorAll("#speech-text [data-sent-idx]").forEach(el => {
    if (String(el.getAttribute("data-sent-idx")) === String(idx)) {
      el.classList.add("speech-sent-current");
    } else {
      el.classList.remove("speech-sent-current");
    }
  });
}


function bindClipWindow(videoEl, detail) {
  const start = Number(detail?.clipStartSeconds);
  const end = Number(detail?.clipEndSeconds);
  if (!videoEl || !Number.isFinite(start) || !Number.isFinite(end) || end <= start) {
    return;
  }

  let initialized = false;
  videoEl.addEventListener("loadedmetadata", () => {
    if (!initialized) {
      initialized = true;
      try {
        videoEl.currentTime = start;
      } catch (_) {
        // ignore browser seeking errors on initial load
      }
    }
  });
  videoEl.addEventListener("timeupdate", () => {
    if (videoEl.currentTime >= end) {
      videoEl.pause();
    }
  });
}

function extractBundestagVideoId(url) {
  if (!url) return "";
  const match = String(url).match(/[?&]videoid=([0-9]+)/i);
  return match ? match[1] : "";
}

let clipEmbedMessageBound = false;

function bindBundestagClipEmbedResize() {
  if (clipEmbedMessageBound) return;
  clipEmbedMessageBound = true;
  window.addEventListener("message", event => {
    const data = event?.data;
    if (!data || data.type !== "mpe-bundestag-player-height") return;
    const iframe = document.getElementById("speech-video-embed");
    if (!iframe) return;
    const height = Number(data.height);
    if (Number.isFinite(height) && height > 0) {
      iframe.style.height = `${Math.max(560, Math.min(height + 16, 1200))}px`;
    }
  });
}

function renderSpeech(detail, toggles) {
  const speakerBox = document.getElementById("speaker-box");
  const videoBox = document.getElementById("video-box");
  const syncNote = document.getElementById("sync-note");
  const speechText = document.getElementById("speech-text");
  const reimportProtocolButton = document.getElementById("reimport-speech-protocol");

  const speaker = detail.speaker || detail.speech?.speaker;
  if (speaker) {
    const name = `${speaker.title || ""} ${speaker.firstName || ""} ${speaker.lastName || ""}`.replace(/\s+/g, " ").trim();
    const faction = speaker.parliamentaryGroup?.shortName || "";
    const imageHtml = speaker.id
      ? `<div class="speaker-image-wrap"><img class="speaker-image" src="/api/deputies/${encodeURIComponent(speaker.id)}/image" alt="${escapeHtml(name || "Speaker")} portrait" loading="lazy" onerror="this.closest('.speaker-box-layout')?.classList.add('speaker-box-no-image'); this.parentElement.style.display='none'"></div>`
      : "";
    const noImageNote = `<div class="meta speaker-image-missing">No portrait available in the Bundestag image database.</div>`;
    speakerBox.innerHTML = `<div class="speaker-box-layout">${imageHtml}<div><strong>Speaker:</strong> ${escapeHtml(name || "Unknown")} ${faction ? `(${escapeHtml(faction)})` : ""}${noImageNote}</div></div>`;
  } else {
    speakerBox.textContent = "Speaker: Unknown";
  }

  const speech = detail.speech || {};
  const comments = Array.isArray(speech.comments) ? speech.comments : [];
  const hasProtocolId = Boolean(speech.protocolId);
  const hasValidAgendaItem = Number(speech.agendaItem) > 0;

  if (reimportProtocolButton) {
    reimportProtocolButton.hidden = !(hasProtocolId && !hasValidAgendaItem);
    reimportProtocolButton.dataset.protocolId = hasProtocolId ? speech.protocolId : "";
  }

  if (detail.video) {
    const streamUrl = detail.video.streamUrl || detail.video.sourceUrl || "";
    const pageUrl = detail.video.videoPageUrl || detail.video.sourceUrl || "";
    const videoId = extractBundestagVideoId(pageUrl);
    const src = escapeHtml(streamUrl);
    const page = escapeHtml(pageUrl);
    const lower = String(streamUrl || "").toLowerCase();
    const isDirectMedia = lower.includes(".mp4") || lower.includes(".m3u8") || lower.includes("/api/videos/") && lower.includes("/file");
    if (isDirectMedia) {
      const isLocalFile = lower.includes("/api/videos/") && lower.includes("/file");
      const clipMeta = !isLocalFile && Number.isFinite(Number(detail.clipStartSeconds)) && Number.isFinite(Number(detail.clipEndSeconds))
        ? `<div class="meta video-note">Playing the detected speech clip from ${Math.round(Number(detail.clipStartSeconds))}s to ${Math.round(Number(detail.clipEndSeconds))}s within the agenda video.</div>`
        : `<div class="meta video-note">Playing the available speech video directly.</div>`;
      videoBox.innerHTML = `<strong>Video:</strong> <a href=\"${page}\" target=\"_blank\" rel=\"noopener\">Open Bundestag video page</a>${clipMeta}<div class="speech-video-stage"><video id=\"speech-video\" controls preload=\"metadata\" class=\"speech-video-player\"><source src=\"${src}\"></video></div>`;
      const videoEl = document.getElementById("speech-video");
      if (!isLocalFile) {
        bindClipWindow(videoEl, detail);
      }
      bindVideoSentimentLamp(videoEl, speech);
    } else if (videoId) {
      const playerSrc = `/api/videos/${encodeURIComponent(detail.video.id)}/embed`;
      videoBox.innerHTML = `<strong>Video:</strong> <a href=\"${page}\" target=\"_blank\" rel=\"noopener\">Open Bundestag video page</a><div class="meta video-note">No local clip file is available for this speech right now. The player below is the Bundestag embed fallback.</div><div class="speech-video-stage speech-video-stage-embed"><iframe id=\"speech-video-embed\" src=\"${escapeHtml(playerSrc)}\" title=\"Bundestag speech clip player\" loading=\"lazy\" allow=\"autoplay; fullscreen\" class=\"speech-video-embed\"></iframe></div>`;
      bindBundestagClipEmbedResize();
    } else {
      videoBox.innerHTML = `<strong>Bundestag Mediathek:</strong> <a href=\"${page}\" target=\"_blank\" rel=\"noopener\">Open video page</a><div class="meta video-note">Only the Bundestag page URL is available for this speech right now. A clip can only be played directly once a stream URL is extracted or a local file is available.</div>`;
    }
  } else {
    videoBox.textContent = "Video: none";
  }

  if (syncNote) {
    const nlp = speech?.nlp || {};
    const syncMode = String(nlp.syncMode || "").toLowerCase();
    const status = String(nlp.timestampStatus || "").toLowerCase();
    const source = String(nlp.timestampSource || "").trim();

    if (!detail.video) {
      syncNote.textContent = "Video sync: no video available.";
    } else if (syncMode === "timed") {
      syncNote.textContent = source
        ? `Video sync: timestamp-based (source: ${source}).`
        : "Video sync: timestamp-based.";
    } else if (syncMode === "approx") {
      syncNote.textContent = "Video sync: approximate (no precise timestamps available for this clip).";
    } else if (status === "missing" || status === "partial") {
      syncNote.textContent = "Video sync: approximate (timestamps incomplete).";
    } else if (status === "already-present" || status === "ok") {
      syncNote.textContent = "Video sync: timestamp-based.";
    } else if (status === "no-sentences") {
      syncNote.textContent = "Video sync: unavailable (no sentence data).";
    } else {
      syncNote.textContent = "";
    }
  }

  const annotations = collectAnnotations(speech, toggles);
  speechText.innerHTML = renderAnnotatedText(speech.text || "", annotations);
  renderCommentList(comments, speech);
}

function setVideoImportStatus(message, type = "") {
  const node = document.getElementById("speech-video-import-status");
  if (!node) return;
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function setNlpRunStatus(message, type = "") {
  const node = document.getElementById("nlp-run-status");
  if (!node) return;
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function setNlpRunBusy(isBusy) {
  const button = document.getElementById("run-nlp-speech");
  if (!button) return;
  button.disabled = Boolean(isBusy);
  button.textContent = isBusy ? "Run DUUI NLP for this speech (running...)" : "Run DUUI NLP for this speech";
}

function getToggles() {
  const thresholdEl = document.getElementById("sarcasm-threshold");
  return {
    comments: document.getElementById("toggle-comments").checked,
    entities: document.getElementById("toggle-entities").checked,
    sarcasm: document.getElementById("toggle-sarcasm").checked,
    sentiment: document.getElementById("toggle-sentiment").checked,
    sarcasmThreshold: Number(thresholdEl?.value ?? 0.45)
  };
}

let currentDetail = null;

async function loadSpeechDetail() {
  const speechId = window.SPEECH_ID;
  // Do not auto-run NLP on page load; detail rendering should stay cheap and explicit.
  currentDetail = await fetchJson(`/api/speeches/${encodeURIComponent(speechId)}/detail?ensureNlp=false`);
  renderSpeech(currentDetail, getToggles());
}

function shouldAutoRunNlp(detail) {
  const speech = detail?.speech || {};
  if (!speech?.id) return false;
  if (!detail?.video) return false;

  if (!speech.nlpProcessed) {
    return true;
  }

  const nlp = speech?.nlp || {};
  const status = String(nlp.timestampStatus || "").toLowerCase();
  const mode = String(nlp.syncMode || "").toLowerCase();
  if (status === "missing" || status === "partial" || status === "no-sentences") {
    return true;
  }
  if (mode !== "timed") {
    return true;
  }

  const rows = Array.isArray(nlp.sentenceSentiments) ? nlp.sentenceSentiments : [];
  if (!rows.length) return true;
  const withT0 = rows.filter(row => Number.isFinite(Number(row?.t0))).length;
  return withT0 / Math.max(1, rows.length) < 0.7;
}

async function runNlpForSpeech(force = true, automated = false, reloadAfter = true) {
  const speechId = window.SPEECH_ID;
  setNlpRunBusy(true);
  setNlpRunStatus(
    automated
      ? "Preparing NLP/timestamps automatically for this speech..."
      : "NLP processing is running...",
    ""
  );
  try {
    // Single-speech runs use the same DUUI-only backend as the dashboard, but force reprocessing by default.
    const result = await fetchJson(`/api/nlp/run/${encodeURIComponent(speechId)}?force=${force ? "true" : "false"}`, { method: "POST" });
    if (reloadAfter) {
      await loadSpeechDetail();
    }
    const processed = Number(result?.processed ?? 0);
    const skipped = Number(result?.skipped ?? 0);
    if (processed > 0) {
      setNlpRunStatus(
        automated
          ? `Automatic NLP/timestamp preparation finished (processed=${processed}, skipped=${skipped}).`
          : `NLP finished successfully (processed=${processed}, skipped=${skipped}). Speech detail was refreshed.`,
        "status-ok"
      );
    } else {
      setNlpRunStatus(
        automated
          ? `Automatic NLP/timestamp preparation finished (processed=${processed}, skipped=${skipped}).`
          : `NLP call finished (processed=${processed}, skipped=${skipped}). Speech detail was refreshed.`,
        ""
      );
    }
  } catch (err) {
    setNlpRunStatus(`Failed to run NLP: ${err.message}`, "status-error");
    throw err;
  } finally {
    setNlpRunBusy(false);
  }
}

async function importVideosForCurrentSpeech() {
  if (!currentDetail?.speech) {
    throw new Error("Speech detail is not loaded yet");
  }
  setVideoImportStatus("Importing video links for this speech's agenda item...");
  const result = await fetchJson(`/api/import/videos/speech/${encodeURIComponent(window.SPEECH_ID)}`, { method: "POST" });
  setVideoImportStatus(`Imported agenda videos: speeches=${result.speechesMatched ?? 0}, stored=${result.videosStored ?? 0}`, "status-ok");
  await loadSpeechDetail();
}

async function reimportProtocolForCurrentSpeech() {
  if (!currentDetail?.speech?.protocolId) {
    throw new Error("This speech has no protocolId");
  }
  const protocolId = currentDetail.speech.protocolId;
  setVideoImportStatus(`Reimporting protocol ${protocolId} with the current parser...`);
  await fetchJson(`/api/import/run/${encodeURIComponent(protocolId)}?force=true`, { method: "POST" });
  setVideoImportStatus(`Reimported protocol ${protocolId}. Retry the video import for this speech.`, "status-ok");
  await loadSpeechDetail();
}

["toggle-comments", "toggle-entities", "toggle-sarcasm", "toggle-sentiment"].forEach(id => {
  document.getElementById(id).addEventListener("change", () => {
    if (!currentDetail) return;
    renderSpeech(currentDetail, getToggles());
  });
});

const sarcasmThreshold = document.getElementById("sarcasm-threshold");
const sarcasmThresholdValue = document.getElementById("sarcasm-threshold-value");
if (sarcasmThreshold && sarcasmThresholdValue) {
  const updateSarcasmThresholdUi = () => {
    const value = Number(sarcasmThreshold.value || 0);
    sarcasmThresholdValue.textContent = value.toFixed(2);
    if (currentDetail) {
      renderSpeech(currentDetail, getToggles());
    }
  };
  sarcasmThreshold.addEventListener("input", updateSarcasmThresholdUi);
  updateSarcasmThresholdUi();
}

document.getElementById("run-nlp-speech").addEventListener("click", () => {
  runNlpForSpeech().catch(err => {
    setNlpRunStatus(`Failed to run NLP: ${err.message}`, "status-error");
  });
});

document.getElementById("import-speech-agenda-videos").addEventListener("click", () => {
  importVideosForCurrentSpeech().catch(err => {
    const message = String(err?.message || "");
    if (message.includes("no imported or downloaded clips found for protocolId=")) {
      setVideoImportStatus(
        `No assignment video bundle is available for this speech yet. ${message.replace("no imported or downloaded clips found for ", "This speech belongs to ")}`,
        ""
      );
      return;
    }
    setVideoImportStatus(`Failed to import speech agenda videos: ${message}`, "status-error");
  });
});

const reimportProtocolButton = document.getElementById("reimport-speech-protocol");
if (reimportProtocolButton) {
  reimportProtocolButton.addEventListener("click", () => {
    reimportProtocolForCurrentSpeech().catch(err => {
      setVideoImportStatus(`Failed to reimport protocol: ${err.message}`, "status-error");
    });
  });
}

loadSpeechDetail().catch(err => {
  document.getElementById("speech-text").textContent = `Failed to load speech detail: ${err.message}`;
});
