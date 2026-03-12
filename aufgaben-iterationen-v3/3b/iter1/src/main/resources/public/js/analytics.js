/* Developer guide: Loads filtered speech data and renders analytics charts (topics, POS, sentiment, entities). */
async function fetchJson(url) {
  const res = await fetch(url);
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

function setStatus(message, type = "") {
  const node = document.getElementById("analytics-status");
  if (!node) return;
  node.className = `meta-note ${type}`.trim();
  node.textContent = message;
}

function setBusy(isBusy) {
  const button = document.getElementById("load-analytics");
  if (button) button.disabled = isBusy;
}

function clearNode(id) {
  const node = document.getElementById(id);
  node.innerHTML = "";
  return node;
}

function emptyViz(id, message) {
  const node = clearNode(id);
  const p = document.createElement("p");
  p.className = "viz-empty";
  p.textContent = message;
  node.appendChild(p);
}

function collectTopicScores(speeches) {
  const map = new Map();
  for (const s of speeches) {
    const topics = s?.nlp?.topics || s?.topics || [];
    if (Array.isArray(topics)) {
      for (const t of topics) {
        const label = t?.label || t?.topic || t?.name;
        const score = Number(t?.score ?? t?.value ?? 0);
        if (!label || Number.isNaN(score)) continue;
        map.set(label, (map.get(label) || 0) + score);
      }
    } else if (topics && typeof topics === "object") {
      for (const [label, value] of Object.entries(topics)) {
        const score = Number(value);
        if (Number.isNaN(score)) continue;
        map.set(label, (map.get(label) || 0) + score);
      }
    }
  }
  return Array.from(map.entries()).map(([label, value]) => ({ label, value })).sort((a, b) => b.value - a.value).slice(0, 8);
}

function collectPosCounts(speeches) {
  const map = new Map();
  for (const s of speeches) {
    const dist = s?.nlp?.posDistribution || s?.posDistribution;
    if (dist && typeof dist === "object") {
      for (const [tag, value] of Object.entries(dist)) {
        const count = Number(value);
        if (!tag || Number.isNaN(count)) continue;
        map.set(tag, (map.get(tag) || 0) + count);
      }
      continue;
    }

    const tokens = s?.nlp?.tokens || s?.tokens || [];
    for (const token of tokens) {
      const tag = token?.pos || token?.tag;
      if (!tag) continue;
      map.set(tag, (map.get(tag) || 0) + 1);
    }
  }
  return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
}

function collectSentiments(speeches) {
  const values = [];
  for (const s of speeches) {
    const arr = s?.nlp?.sentenceSentiments || s?.sentenceSentiments || s?.sentiments || [];
    if (Array.isArray(arr)) {
      for (const item of arr) {
        const v = Number(item?.value ?? item?.score ?? item);
        if (!Number.isNaN(v)) values.push(v);
      }
    }
  }
  return values;
}

function collectEntityCounts(speeches) {
  const map = new Map();
  for (const s of speeches) {
    const ents = s?.nlp?.namedEntities || s?.namedEntities || [];
    for (const e of ents) {
      const type = e?.type || e?.label;
      if (!type) continue;
      map.set(type, (map.get(type) || 0) + 1);
    }
  }
  return Array.from(map.entries()).map(([type, count]) => ({ type, count })).sort((a, b) => b.count - a.count);
}

function drawRadar(id, data) {
  if (!data.length) return emptyViz(id, "No topic annotations available.");

  const node = clearNode(id);
  const width = 420;
  const height = 320;
  const radius = 110;
  const centerX = width / 2;
  const centerY = height / 2;
  const maxV = d3.max(data, d => d.value) || 1;

  const svg = d3.select(node).append("svg").attr("viewBox", `0 0 ${width} ${height}`);
  const g = svg.append("g");

  for (let level = 1; level <= 5; level++) {
    const r = (radius * level) / 5;
    g.append("circle")
      .attr("cx", centerX)
      .attr("cy", centerY)
      .attr("r", r)
      .attr("fill", "none")
      .attr("stroke", "#d7e1e8");
  }

  const angleStep = (Math.PI * 2) / data.length;
  const points = data.map((d, i) => {
    const angle = i * angleStep - Math.PI / 2;
    const r = (d.value / maxV) * radius;
    return [centerX + Math.cos(angle) * r, centerY + Math.sin(angle) * r];
  });

  data.forEach((d, i) => {
    const angle = i * angleStep - Math.PI / 2;
    const lx = centerX + Math.cos(angle) * (radius + 16);
    const ly = centerY + Math.sin(angle) * (radius + 16);
    g.append("text")
      .attr("x", lx)
      .attr("y", ly)
      .attr("text-anchor", "middle")
      .attr("font-size", "11px")
      .text(d.label);
  });

  g.append("polygon")
    .attr("points", points.map(p => p.join(",")).join(" "))
    .attr("fill", "rgba(0, 105, 92, 0.25)")
    .attr("stroke", "#00695c")
    .attr("stroke-width", 2);
}

function drawSunburst(id, data) {
  if (!data.length) return emptyViz(id, "No POS annotations available.");

  const node = clearNode(id);
  const width = 420;
  const radius = 130;

  const root = d3.hierarchy({ name: "POS", children: data.map(d => ({ name: d.name, value: d.value })) })
    .sum(d => d.value);

  d3.partition().size([2 * Math.PI, radius])(root);

  const color = d3.scaleOrdinal()
    .domain(data.map(d => d.name))
    .range(d3.quantize(d3.interpolateGnBu, data.length + 1));

  const arc = d3.arc()
    .startAngle(d => d.x0)
    .endAngle(d => d.x1)
    .innerRadius(d => d.y0)
    .outerRadius(d => d.y1);

  const svg = d3.select(node)
    .append("svg")
    .attr("viewBox", `0 0 ${width} ${width}`)
    .append("g")
    .attr("transform", `translate(${width / 2},${width / 2})`);

  svg.selectAll("path")
    .data(root.descendants().filter(d => d.depth > 0))
    .join("path")
    .attr("d", arc)
    .attr("fill", d => color(d.data.name))
    .append("title")
    .text(d => `${d.data.name}: ${d.value}`);
}

function drawSentimentLine(id, values) {
  if (!values.length) return emptyViz(id, "No sentiment annotations available.");

  const node = clearNode(id);
  const width = 460;
  const height = 260;
  const margin = { top: 16, right: 20, bottom: 26, left: 34 };

  const data = values.map((v, i) => ({ x: i, y: v }));
  const minY = d3.min(data, d => d.y);
  const maxY = d3.max(data, d => d.y);

  const x = d3.scaleLinear().domain([0, data.length - 1]).range([margin.left, width - margin.right]);
  const y = d3.scaleLinear().domain([Math.min(-1, minY), Math.max(1, maxY)]).nice().range([height - margin.bottom, margin.top]);

  const svg = d3.select(node).append("svg").attr("viewBox", `0 0 ${width} ${height}`);

  const line = d3.line().x(d => x(d.x)).y(d => y(d.y));
  svg.append("path")
    .datum(data)
    .attr("fill", "none")
    .attr("stroke", "#00796b")
    .attr("stroke-width", 2)
    .attr("d", line);

  svg.append("g").attr("transform", `translate(0,${height - margin.bottom})`).call(d3.axisBottom(x).ticks(6));
  svg.append("g").attr("transform", `translate(${margin.left},0)`).call(d3.axisLeft(y));
}

function drawEntityBar(id, entities) {
  if (!entities.length) return emptyViz(id, "No named entity annotations available.");

  const node = clearNode(id);
  const width = 460;
  const barHeight = 24;
  const data = entities.slice(0, 10);
  const height = data.length * barHeight + 50;

  const x = d3.scaleLinear().domain([0, d3.max(data, d => d.count) || 1]).range([120, width - 20]);
  const y = d3.scaleBand().domain(data.map(d => d.type)).range([20, height - 20]).padding(0.2);

  const svg = d3.select(node).append("svg").attr("viewBox", `0 0 ${width} ${height}`);
  svg.selectAll("rect")
    .data(data)
    .join("rect")
    .attr("x", 120)
    .attr("y", d => y(d.type))
    .attr("width", d => x(d.count) - 120)
    .attr("height", y.bandwidth())
    .attr("fill", "#00897b");

  svg.selectAll("text.label")
    .data(data)
    .join("text")
    .attr("class", "label")
    .attr("x", 10)
    .attr("y", d => (y(d.type) || 0) + y.bandwidth() / 2 + 4)
    .text(d => d.type)
    .attr("font-size", "11px");

  svg.selectAll("text.value")
    .data(data)
    .join("text")
    .attr("class", "value")
    .attr("x", d => x(d.count) + 6)
    .attr("y", d => (y(d.type) || 0) + y.bandwidth() / 2 + 4)
    .text(d => d.count)
    .attr("font-size", "11px");
}

async function loadAnalytics() {
  const protocolId = document.getElementById("protocolId").value.trim();
  const protocolIds = document.getElementById("protocolIds").value.trim();
  const speechId = document.getElementById("speechId").value.trim();
  const faction = document.getElementById("faction").value.trim();
  const topic = document.getElementById("topic").value.trim();
  const from = document.getElementById("from").value.trim();
  const to = document.getElementById("to").value.trim();
  const matchMode = document.getElementById("matchMode").value;
  const limit = document.getElementById("limit").value || "200";

  setBusy(true);
  setStatus("Loading analytics...");

  try {
    let speeches = [];
    if (speechId) {
      const detail = await fetchJson(`/api/speeches/${encodeURIComponent(speechId)}/detail`);
      if (detail?.speech) speeches = [detail.speech];
    } else {
      const params = new URLSearchParams();
      if (protocolId) params.set("protocolId", protocolId);
      if (protocolIds) params.set("protocolIds", protocolIds);
      if (faction) params.set("faction", faction);
      if (topic) params.set("topic", topic);
      if (from) params.set("from", from);
      if (to) params.set("to", to);
      if (matchMode) params.set("matchMode", matchMode);
      params.set("limit", limit);
      speeches = await fetchJson(`/api/speeches?${params.toString()}`);
    }

    drawRadar("topics-radar", collectTopicScores(speeches));
    drawSunburst("pos-sunburst", collectPosCounts(speeches));
    drawSentimentLine("sentiment-line", collectSentiments(speeches));
    drawEntityBar("entities-bar", collectEntityCounts(speeches));
    setStatus(`Loaded analytics for ${Array.isArray(speeches) ? speeches.length : 0} speeches.`, "status-ok");
  } catch (err) {
    ["topics-radar", "pos-sunburst", "sentiment-line", "entities-bar"].forEach(id => emptyViz(id, `Error: ${err.message}`));
    setStatus(`Failed to load analytics: ${err.message}`, "status-error");
    throw err;
  } finally {
    setBusy(false);
  }
}

async function loadTopicOptions() {
  try {
    const rows = await fetchJson("/api/topics?limit=100");
    const select = document.getElementById("topic");
    const current = select.value;
    select.innerHTML = `<option value="">All</option>`;
    rows.forEach(row => {
      const value = row._id || row.label;
      if (!value) return;
      const option = document.createElement("option");
      option.value = value;
      option.textContent = row.count ? `${value} (${row.count})` : value;
      select.appendChild(option);
    });
    if (current) select.value = current;
  } catch {
    // No topic options available is fine before NLP import.
  }
}

document.getElementById("load-analytics").addEventListener("click", () => {
  loadAnalytics().catch(() => {
    // Message already shown in status line.
  });
});

Promise.all([loadTopicOptions(), loadAnalytics()]).catch(() => {
  // Message already shown in status line.
});
