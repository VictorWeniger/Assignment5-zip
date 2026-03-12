/* Developer guide: Home dashboard actions for import, NLP run, NLP import, and live status updates. */
async function fetchJson(url, options = {}) {
  const res = await fetch(url, options);
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

function setOutput(value) {
  document.getElementById("quick-output").textContent = JSON.stringify(value, null, 2);
}

let importPollingHandle = null;

function setImportStatus(kind, message) {
  const el = document.getElementById("import-status");
  el.className = `import-status ${kind}`;
  el.textContent = message;
}

function setNlpImportStatus(kind, message) {
  const el = document.getElementById("nlp-import-status");
  el.className = `import-status ${kind}`;
  el.textContent = message;
}

function setDuuiRunStatus(kind, message) {
  const el = document.getElementById("duui-run-status");
  el.className = `import-status ${kind}`;
  el.textContent = message;
}

function setVideoCandidateStatus(kind, message) {
  const el = document.getElementById("video-candidate-status");
  el.className = `import-status ${kind}`;
  el.textContent = message;
}

async function loadStats() {
  try {
    const [stats, nlpStats] = await Promise.all([
      fetchJson("/api/stats"),
      fetchJson("/api/nlp/stats")
    ]);

    const merged = { ...stats, ...nlpStats };
    const container = document.getElementById("stats-grid");
    container.innerHTML = "";
    Object.entries(merged).forEach(([key, value]) => {
      const el = document.createElement("div");
      el.className = "stat";
      el.innerHTML = `<div class="k">${key}</div><div class="v">${value}</div>`;
      container.appendChild(el);
    });
  } catch (err) {
    setOutput({ error: err.message });
  }
}

function startImportPolling() {
  stopImportPolling();
  importPollingHandle = setInterval(() => {
    loadStats();
  }, 5000);
}

function stopImportPolling() {
  if (importPollingHandle !== null) {
    clearInterval(importPollingHandle);
    importPollingHandle = null;
  }
}

function flushUi() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => {
      requestAnimationFrame(resolve);
    });
  });
}

function renderVideoCandidates(candidates) {
  const container = document.getElementById("video-candidate-list");
  container.innerHTML = "";
  if (!Array.isArray(candidates) || !candidates.length) {
    setVideoCandidateStatus("idle", "No suitable protocol agenda item found yet.");
    return;
  }

  setVideoCandidateStatus("success", `Loaded ${candidates.length} suggested protocol agenda item candidates.`);
  candidates.forEach((candidate, index) => {
    const el = document.createElement("div");
    el.className = "item";
    const label = candidate.agendaLabel && candidate.agendaLabel.trim()
      ? candidate.agendaLabel
      : "No agenda label parsed yet";
    el.innerHTML = `
      <p><strong>#${index + 1} Protocol ${candidate.protocolId}, TOP ${candidate.agendaItem}</strong></p>
      <p>${label}</p>
      <p class="meta">Session ${candidate.sessionNumber} | speeches=${candidate.speechCount} | speakers=${candidate.speakerCount}</p>
      <div class="actions">
        <button type="button" class="use-video-candidate">Use Candidate</button>
        <a href="${candidate.speechesUrl}"><button type="button">Open Speeches</button></a>
      </div>
    `;
    el.querySelector(".use-video-candidate").addEventListener("click", () => {
      document.getElementById("video-protocol-id").value = candidate.protocolId || "";
      document.getElementById("video-agenda-item").value = candidate.agendaItem || "";
      setVideoCandidateStatus("success", `Selected protocol ${candidate.protocolId}, TOP ${candidate.agendaItem}. Paste the session mediathek URL and start the import.`);
    });
    container.appendChild(el);
  });
}

async function loadVideoCandidates() {
  const button = document.getElementById("refresh-video-candidates");
  const originalLabel = button.textContent;
  try {
    button.disabled = true;
    button.textContent = "Find Candidate (running...)";
    setVideoCandidateStatus("running", "Searching imported speeches for a good protocol agenda item candidate.");
    await flushUi();
    const candidates = await fetchJson("/api/video-candidates?limit=5&minSpeeches=3");
    renderVideoCandidates(candidates);
  } catch (err) {
    document.getElementById("video-candidate-list").innerHTML = "";
    setVideoCandidateStatus("error", `Could not load video candidates: ${err.message}`);
  } finally {
    button.textContent = originalLabel;
    button.disabled = false;
  }
}

document.getElementById("preview-import").addEventListener("click", async () => {
  try {
    const data = await fetchJson("/api/import/preview?period=20&limit=3");
    setOutput(data);
  } catch (err) {
    setOutput({ error: err.message });
  }
});

async function runProtocolImport(limit) {
  const buttons = [
    document.getElementById("run-import-3"),
    document.getElementById("run-import-10"),
    document.getElementById("run-import-all")
  ];
  try {
    buttons.forEach((button) => {
      button.disabled = true;
    });
    setImportStatus("running", "Protocol import is running. Stats refresh every 5 seconds.");
    startImportPolling();
    await flushUi();
    const url = limit > 0
      ? `/api/import/run?period=20&limit=${limit}`
      : "/api/import/run?period=20";
    const data = await fetchJson(url, { method: "POST" });
    setOutput(data);
    await loadStats();
    setImportStatus("success", "Protocol import finished.");
  } catch (err) {
    setOutput({ error: err.message });
    if (err.message.includes("already running")) {
      setImportStatus("running", "A protocol import is already running. Stats refresh every 5 seconds.");
      startImportPolling();
    } else {
      setImportStatus("error", `Protocol import failed: ${err.message}`);
    }
  } finally {
    if (!document.getElementById("import-status").classList.contains("running")) {
      stopImportPolling();
    }
    buttons.forEach((button) => {
      button.disabled = false;
    });
  }
}

document.getElementById("run-import-3").addEventListener("click", async () => {
  await runProtocolImport(3);
});

document.getElementById("run-import-10").addEventListener("click", async () => {
  await runProtocolImport(10);
});

document.getElementById("run-import-all").addEventListener("click", async () => {
  await runProtocolImport(0);
});

async function runDuuiNlp(limit) {
  const buttons = [
    document.getElementById("run-nlp-3"),
    document.getElementById("run-nlp-300"),
    document.getElementById("run-nlp-all")
  ];
  try {
    buttons.forEach(button => {
      button.disabled = true;
    });
    setDuuiRunStatus("running", `DUUI NLP batch processing is running${limit > 0 ? ` (limit=${limit})` : " for all pending speeches"}.`);
    startImportPolling();
    await flushUi();
    // The three dashboard buttons only differ by limit; all of them hit the same DUUI-only backend route.
    const url = limit > 0 ? `/api/nlp/run?limit=${limit}` : "/api/nlp/run";
    const data = await fetchJson(url, { method: "POST" });
    setOutput(data);
    await loadStats();
    setDuuiRunStatus("success", `DUUI NLP batch processing finished${limit > 0 ? ` (limit=${limit})` : " for all pending speeches"}.`);
  } catch (err) {
    setOutput({ error: err.message });
    setDuuiRunStatus("error", `DUUI NLP batch processing failed: ${err.message}`);
  } finally {
    stopImportPolling();
    buttons.forEach(button => {
      button.disabled = false;
    });
  }
}

document.getElementById("run-nlp-3").addEventListener("click", async () => {
  await runDuuiNlp(3);
});

document.getElementById("run-nlp-300").addEventListener("click", async () => {
  await runDuuiNlp(300);
});

document.getElementById("run-nlp-all").addEventListener("click", async () => {
  await runDuuiNlp(0);
});

document.getElementById("import-nlp-file").addEventListener("click", async () => {
  const button = document.getElementById("import-nlp-file");
  const originalLabel = button.textContent;
  try {
    button.disabled = true;
    button.textContent = "Import Professor NLP XMI Files (running...)";
    setNlpImportStatus("running", "Professor NLP XMI import is running. Stats refresh every 5 seconds.");
    startImportPolling();
    await flushUi();
    const data = await fetchJson("/api/nlp/import?createMissing=false", { method: "POST" });
    setOutput(data);
    await loadStats();
    const result = data.result || {};
    setNlpImportStatus(
      "success",
      `Professor NLP XMI import finished. processed=${result.processed ?? 0}, updated=${result.updated ?? 0}, created=${result.created ?? 0}, skipped=${result.skipped ?? 0}`
    );
  } catch (err) {
    setOutput({ error: err.message });
    setNlpImportStatus("error", `Professor NLP XMI import failed: ${err.message}`);
  } finally {
    stopImportPolling();
    button.textContent = originalLabel;
    button.disabled = false;
  }
});

document.getElementById("refresh-video-candidates").addEventListener("click", async () => {
  await loadVideoCandidates();
});

document.getElementById("import-agenda-videos").addEventListener("click", async () => {
  const button = document.getElementById("import-agenda-videos");
  const originalLabel = button.textContent;
  try {
    const protocolId = document.getElementById("video-protocol-id").value.trim();
    const agendaItem = document.getElementById("video-agenda-item").value.trim();
    const sessionUrl = document.getElementById("video-session-url").value.trim();
    const maxSpeeches = document.getElementById("video-max-speeches").value.trim();
    button.disabled = true;
    button.textContent = "Import Agenda Videos (running...)";
    setVideoCandidateStatus("running", "Resolving video links from the Bundestag mediathek session page.");
    await flushUi();
    const params = new URLSearchParams({
      protocolId,
      agendaItem,
      sessionUrl,
      maxSpeeches
    });
    const data = await fetchJson(`/api/import/videos/agenda?${params.toString()}`, { method: "POST" });
    setOutput(data);
    await loadStats();
    setVideoCandidateStatus(
      "success",
      `Agenda videos imported. speeches=${data.speechesMatched ?? 0}, stored=${data.videosStored ?? 0}, downloadedFiles=${data.downloadedFiles ?? 0}`
    );
  } catch (err) {
    setOutput({ error: err.message });
    setVideoCandidateStatus("error", `Agenda video import failed: ${err.message}`);
  } finally {
    button.textContent = originalLabel;
    button.disabled = false;
  }
});

document.getElementById("auto-import-agenda-videos").addEventListener("click", async () => {
  const button = document.getElementById("auto-import-agenda-videos");
  const originalLabel = button.textContent;
  try {
    const sessionUrl = document.getElementById("video-session-url").value.trim();
    const maxSpeeches = document.getElementById("video-max-speeches").value.trim();
    button.disabled = true;
    button.textContent = "Auto Import Agenda Videos (running...)";
    setVideoCandidateStatus("running", "Matching the mediathek session page to one imported session and importing a small agenda subset.");
    await flushUi();
    const params = new URLSearchParams({
      sessionUrl,
      maxSpeeches
    });
    const data = await fetchJson(`/api/import/videos/auto?${params.toString()}`, { method: "POST" });
    setOutput(data);
    document.getElementById("video-protocol-id").value = data.protocolId || "";
    document.getElementById("video-agenda-item").value = data.agendaItem || "";
    await loadStats();
    setVideoCandidateStatus(
      "success",
      `Auto agenda video import finished. protocol=${data.protocolId}, top=${data.agendaItem}, speeches=${data.speechesMatched ?? 0}, stored=${data.videosStored ?? 0}`
    );
  } catch (err) {
    setOutput({ error: err.message });
    setVideoCandidateStatus("error", `Auto agenda video import failed: ${err.message}`);
  } finally {
    button.textContent = originalLabel;
    button.disabled = false;
  }
});

loadStats();
loadVideoCandidates();
