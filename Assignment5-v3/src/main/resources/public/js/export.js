/* Developer guide: Handles export UI actions for TeX/PDF generation and template editing workflow. */
function setStatus(message, type = "") {
  const node = document.getElementById("export-status");
  if (!node) return;
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function setBusy(isBusy) {
  ["generate-tex", "open-pdf", "seed-templates", "load-template", "save-template"].forEach(id => {
    const node = document.getElementById(id);
    if (node) node.disabled = isBusy;
  });
}

function buildParams() {
  const params = new URLSearchParams();
  const title = document.getElementById("title").value.trim();
  const protocolId = document.getElementById("protocolId").value.trim();
  const protocolIds = document.getElementById("protocolIds").value.trim();
  const speakerId = document.getElementById("speakerId").value.trim();
  const faction = document.getElementById("faction").value.trim();
  const topic = document.getElementById("topic").value.trim();
  const matchMode = document.getElementById("matchMode").value;
  const groupBy = document.getElementById("groupBy").value;
  const from = document.getElementById("from").value.trim();
  const to = document.getElementById("to").value.trim();
  const includeTikz = document.getElementById("includeTikz").checked;
  const limit = document.getElementById("limit").value || "200";

  if (title) params.set("title", title);
  if (protocolId) params.set("protocolId", protocolId);
  if (protocolIds) params.set("protocolIds", protocolIds);
  if (speakerId) params.set("speakerId", speakerId);
  if (faction) params.set("faction", faction);
  if (topic) params.set("topic", topic);
  if (matchMode) params.set("matchMode", matchMode);
  if (groupBy) params.set("groupBy", groupBy);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  if (includeTikz) params.set("includeTikz", "true");
  params.set("limit", limit);
  return params;
}

async function loadExport() {
  const params = buildParams();
  const output = document.getElementById("tex-output");
  output.textContent = "Loading...";
  setBusy(true);
  setStatus("Generating TeX...");

  try {
    const res = await fetch(`/api/export/tex?${params.toString()}`);
    const text = await res.text();
    if (!res.ok) {
      output.textContent = `Error: ${text}`;
      setStatus(`TeX generation failed: HTTP ${res.status}`, "status-error");
      return;
    }
    output.textContent = text;
    setStatus("TeX generated successfully.", "status-ok");
  } catch (err) {
    output.textContent = `Error: ${err.message}`;
    setStatus(`TeX generation failed: ${err.message}`, "status-error");
    throw err;
  } finally {
    setBusy(false);
  }
}

function openPdf() {
  const params = buildParams();
  window.open(`/api/export/pdf?${params.toString()}`, "_blank", "noopener");
  setStatus("Opened PDF endpoint in a new tab.", "status-ok");
}

async function seedTemplates() {
  const res = await fetch("/api/templates/seed", { method: "POST" });
  if (!res.ok) {
    const body = await res.text();
    throw new Error(body);
  }
}

async function loadTemplate() {
  const id = document.getElementById("templateId").value.trim();
  if (!id) return;
  const res = await fetch(`/api/templates/${encodeURIComponent(id)}`);
  if (res.status === 404) {
    document.getElementById("templateName").value = "";
    document.getElementById("templateContent").value = "";
    setStatus(`Template '${id}' does not exist yet.`, "status-error");
    return;
  }
  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);

  document.getElementById("templateName").value = data.name || "";
  document.getElementById("templateContent").value = data.content || "";
  setStatus(`Template '${id}' loaded.`, "status-ok");
}

async function saveTemplate() {
  const id = document.getElementById("templateId").value.trim();
  if (!id) {
    throw new Error("templateId is required");
  }

  const payload = {
    name: document.getElementById("templateName").value.trim(),
    content: document.getElementById("templateContent").value
  };

  const res = await fetch(`/api/templates/${encodeURIComponent(id)}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  const data = await res.json();
  if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
  setStatus(`Template '${id}' saved.`, "status-ok");
}

document.getElementById("generate-tex").addEventListener("click", () => {
  loadExport().catch(() => {
    // Message already shown in status line.
  });
});

document.getElementById("open-pdf").addEventListener("click", () => {
  openPdf();
});

document.getElementById("seed-templates").addEventListener("click", () => {
  setBusy(true);
  setStatus("Seeding templates...");
  seedTemplates()
    .then(() => loadTemplate())
    .then(() => setStatus("Templates seeded successfully.", "status-ok"))
    .catch(err => {
      setStatus(`Template seeding failed: ${err.message}`, "status-error");
      document.getElementById("tex-output").textContent = `Error: ${err.message}`;
    })
    .finally(() => setBusy(false));
});

document.getElementById("load-template").addEventListener("click", () => {
  setBusy(true);
  loadTemplate()
    .catch(err => {
      setStatus(`Template load failed: ${err.message}`, "status-error");
      document.getElementById("tex-output").textContent = `Error: ${err.message}`;
    })
    .finally(() => setBusy(false));
});

document.getElementById("save-template").addEventListener("click", () => {
  setBusy(true);
  saveTemplate()
    .catch(err => {
      setStatus(`Template save failed: ${err.message}`, "status-error");
      document.getElementById("tex-output").textContent = `Error: ${err.message}`;
    })
    .finally(() => setBusy(false));
});

Promise.all([loadExport(), loadTemplate()]).catch(() => {
  // Message already shown in status line.
});
