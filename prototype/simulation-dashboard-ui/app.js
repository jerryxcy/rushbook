/*
 * THROWAWAY PROTOTYPE — NEVER MERGE TO MAIN
 * Question: which layout makes Booking → Kafka → Notification easiest to learn?
 */

const VARIANTS = [
  { key: "A", name: "Pipeline Story" },
  { key: "B", name: "Operations Console" },
  { key: "C", name: "Guided Hybrid" },
];

const DEFAULT_CONFIG = {
  capacity: 10,
  attendees: 100,
  scenario: "healthy",
};

const app = document.querySelector("#app");
const variantLabel = document.querySelector("#variant-label");
const timers = new Set();

let state = createInitialState();

function createInitialState(config = DEFAULT_CONFIG) {
  return {
    config: { ...config },
    stage: "idle",
    holds: 0,
    bookings: 0,
    rejected: 0,
    outbox: 0,
    published: 0,
    lag: 0,
    notifications: 0,
    failures: 0,
    logs: [
      {
        time: now(),
        message: "Simulation ready. Choose a scenario and start.",
      },
    ],
  };
}

function now() {
  return new Intl.DateTimeFormat("zh-TW", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(new Date());
}

function getVariant() {
  const requested = new URLSearchParams(window.location.search)
    .get("variant")
    ?.toUpperCase();
  return VARIANTS.some((variant) => variant.key === requested)
    ? requested
    : "A";
}

function setVariant(key) {
  const url = new URL(window.location.href);
  url.searchParams.set("variant", key);
  window.history.replaceState({}, "", url);
  render();
}

function cycleVariant(direction) {
  const index = VARIANTS.findIndex((variant) => variant.key === getVariant());
  const nextIndex = (index + direction + VARIANTS.length) % VARIANTS.length;
  setVariant(VARIANTS[nextIndex].key);
}

function schedule(callback, delay) {
  const timer = window.setTimeout(() => {
    timers.delete(timer);
    callback();
  }, delay);
  timers.add(timer);
}

function clearTimers() {
  for (const timer of timers) {
    window.clearTimeout(timer);
  }
  timers.clear();
}

function updateConfigFromForm() {
  const capacity = Number(document.querySelector("[name=capacity]")?.value);
  const attendees = Number(document.querySelector("[name=attendees]")?.value);
  const scenario = document.querySelector("[name=scenario]")?.value;

  state.config = {
    capacity: Math.max(1, Math.min(capacity || 10, 1000)),
    attendees: Math.max(1, Math.min(attendees || 100, 5000)),
    scenario: scenario || "healthy",
  };
}

function addLog(message) {
  state.logs = [{ time: now(), message }, ...state.logs].slice(0, 7);
}

function startSimulation() {
  clearTimers();
  updateConfigFromForm();
  state = createInitialState(state.config);
  state.stage = "racing";
  addLog(
    `${state.config.attendees} Attendees started competing for ${state.config.capacity} Spots.`,
  );
  render();

  schedule(() => {
    state.holds = Math.min(state.config.capacity, state.config.attendees);
    state.rejected = Math.max(0, state.config.attendees - state.holds);
    state.stage = "booking";
    addLog(
      `PostgreSQL admitted ${state.holds} Holds and rejected ${state.rejected} requests.`,
    );
    render();
  }, 700);

  schedule(() => {
    state.bookings = state.holds;
    state.outbox = state.bookings;
    state.stage = "outbox";
    addLog(
      `${state.bookings} Bookings and ${state.outbox} outbox messages committed together.`,
    );
    render();
  }, 1450);

  schedule(() => {
    if (state.config.scenario === "kafka-outage") {
      state.stage = "kafka-down";
      addLog(
        `Kafka is unavailable. Bookings remain safe; ${state.outbox} messages wait in the outbox.`,
      );
      render();
      return;
    }

    state.published = state.outbox;
    state.outbox = 0;
    state.lag = state.published;
    state.stage =
      state.config.scenario === "consumer-failure"
        ? "consumer-failed"
        : "consuming";
    addLog(`${state.published} BookingConfirmed messages reached Kafka.`);

    if (state.stage === "consumer-failed") {
      state.failures = 1;
      addLog(
        "Notification consumer stopped before committing offsets. Messages remain available.",
      );
    }
    render();

    if (state.stage === "consuming") {
      finishNotifications();
    }
  }, 2300);
}

function restoreKafka() {
  state.stage = "publishing";
  addLog("Kafka restored. Outbox publisher resumed.");
  render();

  schedule(() => {
    state.published = state.outbox;
    state.lag = state.published;
    state.outbox = 0;
    state.stage = "consuming";
    addLog(`${state.published} waiting messages published after recovery.`);
    render();
    finishNotifications();
  }, 850);
}

function resumeConsumer() {
  state.stage = "consuming";
  addLog("Notification consumer restarted. Inbox prevents duplicate effects.");
  render();
  finishNotifications();
}

function finishNotifications() {
  schedule(() => {
    state.notifications = state.bookings;
    state.lag = 0;
    state.stage = "complete";
    addLog(
      `${state.notifications} Notifications recorded. Simulation complete.`,
    );
    render();
  }, 900);
}

function resetSimulation() {
  clearTimers();
  state = createInitialState(state.config);
  render();
}

function primaryAction() {
  if (state.stage === "kafka-down") {
    restoreKafka();
    return;
  }
  if (state.stage === "consumer-failed") {
    resumeConsumer();
    return;
  }
  startSimulation();
}

function primaryActionLabel() {
  if (state.stage === "kafka-down") return "恢復 Kafka";
  if (state.stage === "consumer-failed") return "重啟 Consumer";
  if (isRunning()) return "重新模擬";
  return "開始模擬";
}

function isRunning() {
  return ["racing", "booking", "outbox", "publishing", "consuming"].includes(
    state.stage,
  );
}

function stageMeta() {
  const map = {
    idle: ["準備就緒", "neutral"],
    racing: ["Attendees 正在搶名額", "live"],
    booking: ["PostgreSQL 正在確認", "live"],
    outbox: ["Booking 已安全寫入", "live"],
    publishing: ["Outbox 正在補送", "live"],
    consuming: ["Consumer 正在處理", "live"],
    "kafka-down": ["Kafka 暫時故障", "warning"],
    "consumer-failed": ["Consumer 已停止", "danger"],
    complete: ["流程完成", "live"],
  };
  return map[state.stage] || map.idle;
}

function scenarioLabel() {
  const labels = {
    healthy: "正常流程",
    "kafka-outage": "Kafka 暫時故障",
    "consumer-failure": "Consumer 處理中斷",
  };
  return labels[state.config.scenario];
}

function controlsMarkup(className = "") {
  return `
    <div class="control-grid ${className}">
      <div class="field">
        <label for="capacity">活動名額</label>
        <input id="capacity" name="capacity" type="number" min="1" max="1000"
          value="${state.config.capacity}" />
      </div>
      <div class="field">
        <label for="attendees">同時報名人數</label>
        <input id="attendees" name="attendees" type="number" min="1" max="5000"
          value="${state.config.attendees}" />
      </div>
      <div class="field">
        <label for="scenario">模擬情境</label>
        <select id="scenario" name="scenario">
          ${scenarioOption("healthy", "正常流程")}
          ${scenarioOption("kafka-outage", "Kafka 暫時故障")}
          ${scenarioOption("consumer-failure", "Consumer 處理中斷")}
        </select>
      </div>
    </div>
  `;
}

function scenarioOption(value, label) {
  const selected = state.config.scenario === value ? "selected" : "";
  return `<option value="${value}" ${selected}>${label}</option>`;
}

function actionButtonsMarkup() {
  return `
    <div class="button-row">
      <button class="primary-button" type="button" data-action="primary">
        ${primaryActionLabel()}
      </button>
      <button class="secondary-button" type="button" data-action="reset">
        重設
      </button>
    </div>
  `;
}

function stateLineMarkup(className = "") {
  return `
    <div class="state-line ${className}" aria-label="完整 prototype state">
      <span>Stage <b>${state.stage}</b></span>
      <span>Holds <b>${state.holds}</b></span>
      <span>Bookings <b>${state.bookings}</b></span>
      <span>Rejected <b>${state.rejected}</b></span>
      <span>Outbox <b>${state.outbox}</b></span>
      <span>Kafka lag <b>${state.lag}</b></span>
      <span>Notifications <b>${state.notifications}</b></span>
    </div>
  `;
}

function feedMarkup() {
  return `
    <ul class="feed-list">
      ${state.logs
        .map(
          (entry) => `
            <li>
              <time>${entry.time}</time>
              <span>${entry.message}</span>
            </li>
          `,
        )
        .join("")}
    </ul>
  `;
}

function componentState(component) {
  if (component === "booking") {
    return state.stage === "racing" || state.stage === "booking"
      ? ["Processing", "live"]
      : ["Ready", "live"];
  }
  if (component === "postgres") return ["Healthy", "live"];
  if (component === "kafka") {
    return state.stage === "kafka-down"
      ? ["Unavailable", "danger"]
      : ["Healthy", "live"];
  }
  if (component === "notification") {
    return state.stage === "consumer-failed"
      ? ["Stopped", "danger"]
      : ["Healthy", "live"];
  }
  return ["Unknown", "neutral"];
}

function statusMarkup(component) {
  const [label, tone] = componentState(component);
  return `<span><i class="status-dot ${tone}"></i>${label}</span>`;
}

function stageClass(names) {
  return names.includes(state.stage) ? "active" : "";
}

function problemClass(names) {
  return names.includes(state.stage) ? "problem" : "";
}

function explanationText() {
  if (state.stage === "kafka-down") {
    return "Booking 已經成功，不需要跟著 Kafka 一起失敗。Outbox 保存尚未發布的事件，等待 Kafka 恢復。";
  }
  if (state.stage === "consumer-failed") {
    return "Kafka message 還沒有完成 offset commit，因此可以重新傳遞；Notification Inbox 會阻止重複效果。";
  }
  if (state.stage === "complete") {
    return "PostgreSQL 決定名額，Kafka 傳遞結果，Notification 完成後續工作。每個元件只負責自己的部分。";
  }
  if (state.stage === "idle") {
    return "先選擇情境，再觀察資料如何從 Booking 流向 Notification。";
  }
  return "PostgreSQL 正在保護名額；Booking 成功後，Kafka 才接手非同步傳遞。";
}

function renderBanner() {
  const [label, tone] = stageMeta();
  return `
    <div class="prototype-banner">
      <span><strong>THROWAWAY</strong> · 只用來選擇 UI 方向，不接真實系統</span>
      <span><i class="status-dot ${tone}"></i>${label} · ${scenarioLabel()}</span>
    </div>
  `;
}

function renderVariantA() {
  return `
    <section class="variant-a">
      ${renderBanner()}
      <div class="a-shell">
        <header class="a-header">
          <div>
            <span class="eyebrow">Variant A · Pipeline Story</span>
            <h1>一眼看懂這次搶票發生了什麼。</h1>
            <p>
              用四個步驟呈現資料流。每個數字都回答一個問題：
              誰來了、誰成功、訊息在哪裡、通知完成了嗎？
            </p>
          </div>
          <div class="a-run-state">
            <i class="status-dot ${stageMeta()[1]}"></i>
            <strong>${stageMeta()[0]}</strong>
            <div style="margin-top:8px;color:#718078">
              ${state.config.attendees} 人競爭 ${state.config.capacity} 個名額
            </div>
          </div>
        </header>

        <section class="a-controls">
          ${controlsMarkup()}
          ${actionButtonsMarkup()}
        </section>

        <section class="a-pipeline" aria-label="Booking event pipeline">
          <article class="a-stage ${stageClass(["racing"])}">
            <span class="stage-number">01</span>
            <p class="stage-value">${state.config.attendees}</p>
            <h2 class="stage-title">Attendees</h2>
            <p class="stage-copy">
              同時向 Booking Service 要求 Hold。
            </p>
          </article>
          <article class="a-stage ${stageClass(["booking", "outbox"])}">
            <span class="stage-number">02</span>
            <p class="stage-value">${state.bookings}</p>
            <h2 class="stage-title">Bookings</h2>
            <p class="stage-copy">
              PostgreSQL 最多只允許 ${state.config.capacity} 個成功。
            </p>
          </article>
          <article class="a-stage
            ${stageClass(["publishing", "consuming"])}
            ${problemClass(["kafka-down"])}">
            <span class="stage-number">03</span>
            <p class="stage-value">${state.outbox || state.lag}</p>
            <h2 class="stage-title">Messages waiting</h2>
            <p class="stage-copy">
              Outbox 與 Kafka 確保成功事件不會消失。
            </p>
          </article>
          <article class="a-stage
            ${stageClass(["complete"])}
            ${problemClass(["consumer-failed"])}">
            <span class="stage-number">04</span>
            <p class="stage-value">${state.notifications}</p>
            <h2 class="stage-title">Notifications</h2>
            <p class="stage-copy">
              Consumer 處理 BookingConfirmed。
            </p>
          </article>
        </section>

        <section class="a-explanation">
          <article class="lesson-card">
            <span class="eyebrow">現在最重要的觀察</span>
            <h2>${explanationText()}</h2>
            <p>
              ${state.rejected} 個 request 沒有取得 Spot；
              ${state.bookings} 筆 Booking 已安全保存。
            </p>
          </article>
          <article class="event-feed">
            <h3>剛才發生的事</h3>
            ${feedMarkup()}
          </article>
        </section>

        ${stateLineMarkup("c-state")}
      </div>
    </section>
  `;
}

function renderVariantB() {
  const logText = state.logs
    .map((entry) => `${entry.time}  ${entry.message}`)
    .join("\n");
  const chartValues = [18, 26, 34, 46, 38, 60, 82, 100, 74, 52, 28, 16];

  return `
    <section class="variant-b">
      ${renderBanner()}
      <div class="b-layout">
        <aside class="b-sidebar">
          <div class="b-brand">
            <span class="b-brand-mark">R</span>
            <span>RushBook Ops</span>
          </div>
          <h2>Load generator</h2>
          <p>
            Configure traffic and inject one application-level failure.
          </p>
          ${controlsMarkup()}
          <div style="margin-top:14px">${actionButtonsMarkup()}</div>

          <div class="b-component-list">
            ${componentRow("Booking Service", "booking")}
            ${componentRow("PostgreSQL", "postgres")}
            ${componentRow("Kafka", "kafka")}
            ${componentRow("Notification", "notification")}
          </div>
        </aside>

        <section class="b-main">
          <header class="b-header">
            <div>
              <h1>Summer Meetup / Live run</h1>
              <p>event_01 · ${scenarioLabel()} · stage=${state.stage}</p>
            </div>
            <span class="b-health">
              <i class="status-dot ${stageMeta()[1]}"></i>${stageMeta()[0]}
            </span>
          </header>

          <section class="b-metrics">
            ${opsMetric("BOOKING SUCCESS", state.bookings, `${state.config.capacity} capacity`)}
            ${opsMetric("REJECTED", state.rejected, "capacity protected")}
            ${opsMetric("OUTBOX BACKLOG", state.outbox, "unpublished messages")}
            ${opsMetric("CONSUMER LAG", state.lag, "booking-confirmed.v1")}
          </section>

          <section class="b-grid">
            <article class="b-panel">
              <header class="b-panel-header">
                <h3>Registration throughput</h3>
                <span>requests / 250 ms</span>
              </header>
              <div class="throughput-chart" aria-label="Mock throughput chart">
                ${chartValues
                  .map(
                    (value, index) =>
                      `<i class="${index === 7 ? "hot" : ""}" style="height:${value}%"></i>`,
                  )
                  .join("")}
              </div>
            </article>

            <article class="b-panel">
              <header class="b-panel-header">
                <h3>Kafka partitions</h3>
                <span>key = bookingId</span>
              </header>
              ${partitionRow("P0", Math.min(100, state.lag * 9 + 6), Math.ceil(state.lag / 3))}
              ${partitionRow("P1", Math.min(100, state.lag * 7 + 4), Math.floor(state.lag / 3))}
              ${partitionRow("P2", Math.min(100, state.lag * 8 + 5), Math.floor(state.lag / 3))}
            </article>

            <article class="b-panel">
              <header class="b-panel-header">
                <h3>Event stream</h3>
                <span>latest first</span>
              </header>
              <pre class="b-log">${logText}</pre>
            </article>

            <article class="b-panel">
              <header class="b-panel-header">
                <h3>Delivery state</h3>
                <span>at-least-once</span>
              </header>
              ${partitionRow("Published", progress(state.published, state.bookings), state.published)}
              ${partitionRow("Consumed", progress(state.notifications, state.bookings), state.notifications)}
              ${partitionRow("Failed", state.failures ? 100 : 0, state.failures)}
            </article>
          </section>

          ${stateLineMarkup("b-state")}
        </section>
      </div>
    </section>
  `;
}

function componentRow(label, component) {
  return `
    <div class="b-component">
      <b>${label}</b>
      ${statusMarkup(component)}
    </div>
  `;
}

function opsMetric(label, value, detail) {
  return `
    <article class="b-metric">
      <span>${label}</span>
      <strong>${value}</strong>
      <small>${detail}</small>
    </article>
  `;
}

function partitionRow(label, width, value) {
  return `
    <div class="partition-row">
      <span>${label}</span>
      <div class="partition-track"><span style="width:${width}%"></span></div>
      <b>${value}</b>
    </div>
  `;
}

function progress(value, total) {
  if (total === 0) return 0;
  return Math.round((value / total) * 100);
}

function journeyState(stageNumber) {
  const order = {
    idle: 0,
    racing: 1,
    booking: 1,
    outbox: 2,
    "kafka-down": 2,
    publishing: 3,
    consuming: 3,
    "consumer-failed": 3,
    complete: 4,
  };
  const current = order[state.stage] || 0;
  if (
    (stageNumber === 3 && state.stage === "kafka-down") ||
    (stageNumber === 4 && state.stage === "consumer-failed")
  ) {
    return "problem";
  }
  if (current > stageNumber) return "done";
  if (current === stageNumber) return "active";
  return "";
}

function renderVariantC() {
  return `
    <section class="variant-c">
      ${renderBanner()}
      <div class="c-shell">
        <nav class="c-nav">
          <span class="c-brand">RushBook</span>
          <span class="c-nav-note">SIMULATION / SUMMER MEETUP</span>
        </nav>

        <section class="c-hero">
          <div class="c-intro">
            <span class="eyebrow">Variant C · Guided Hybrid</span>
            <h1>看見一次 Booking 如何穿過整個系統。</h1>
            <p>
              設定人數與 failure，按下模擬。上方先說結果，
              下方再解釋 PostgreSQL、Kafka 與 Notification 各自做了什麼。
            </p>
            ${controlsMarkup("c-form")}
            <div class="c-actions">${actionButtonsMarkup()}</div>
          </div>

          <aside class="c-result">
            <div>
              <span class="c-result-label">Confirmed Bookings</span>
              <strong>${state.bookings}</strong>
              <p>${explanationText()}</p>
            </div>
            <div class="c-mini-stats">
              <div class="c-mini-stat">
                <span>Rejected</span>
                <b>${state.rejected}</b>
              </div>
              <div class="c-mini-stat">
                <span>Notifications</span>
                <b>${state.notifications}</b>
              </div>
            </div>
          </aside>
        </section>

        <section class="c-journey">
          <header class="c-section-head">
            <h2>這次事件的旅程</h2>
            <p>
              預設只顯示學習需要的四個步驟；更深入的 infrastructure
              metrics 交給 Grafana。
            </p>
          </header>
          <div class="journey-track">
            ${journeyNode(1, "Request", `${state.config.attendees} 位 Attendees 同時要求 Hold。`)}
            ${journeyNode(2, "PostgreSQL", `${state.bookings} 筆 Booking 安全 commit。`)}
            ${journeyNode(3, "Kafka", `${state.outbox + state.lag} 則訊息等待或傳遞中。`)}
            ${journeyNode(4, "Notification", `${state.notifications} 筆通知效果完成。`)}
          </div>
        </section>

        <section class="c-lower">
          <article class="c-story">
            <h3>你現在應該注意什麼？</h3>
            <div class="c-story-callout">${explanationText()}</div>
            ${stateLineMarkup("c-state")}
          </article>
          <article class="c-feed">
            <h3>最近事件</h3>
            ${feedMarkup()}
          </article>
        </section>
      </div>
    </section>
  `;
}

function journeyNode(number, title, copy) {
  return `
    <article class="journey-node ${journeyState(number)}">
      <span class="journey-dot">${number}</span>
      <h3>${title}</h3>
      <p>${copy}</p>
    </article>
  `;
}

function render() {
  const variant = getVariant();
  const metadata = VARIANTS.find((item) => item.key === variant);
  variantLabel.textContent = `${metadata.key} — ${metadata.name}`;

  if (variant === "B") {
    app.innerHTML = renderVariantB();
  } else if (variant === "C") {
    app.innerHTML = renderVariantC();
  } else {
    app.innerHTML = renderVariantA();
  }

  bindControls();
}

function bindControls() {
  document
    .querySelector("[data-action=primary]")
    ?.addEventListener("click", primaryAction);
  document
    .querySelector("[data-action=reset]")
    ?.addEventListener("click", resetSimulation);
}

document
  .querySelector("#previous-variant")
  .addEventListener("click", () => cycleVariant(-1));
document
  .querySelector("#next-variant")
  .addEventListener("click", () => cycleVariant(1));

window.addEventListener("keydown", (event) => {
  const target = event.target;
  if (
    target instanceof HTMLInputElement ||
    target instanceof HTMLTextAreaElement ||
    target instanceof HTMLSelectElement ||
    target?.isContentEditable
  ) {
    return;
  }

  if (event.key === "ArrowLeft") cycleVariant(-1);
  if (event.key === "ArrowRight") cycleVariant(1);
});

window.addEventListener("popstate", render);
render();
