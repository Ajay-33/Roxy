import { stateFor } from "./dashboard-state.js";

const view = document.querySelector("#dashboard");
const picker = document.querySelector("#state-picker");

function list(items, fallback, renderItem) {
  return items.length === 0 ? `<p class="empty">${fallback}</p>` : items.map(renderItem).join("");
}

function render(name) {
  const state = stateFor(name);
  view.innerHTML = `
    <section class="hero" aria-labelledby="page-title">
      <p class="eyebrow">${state.eyebrow}</p>
      <h1 id="page-title">${state.title}</h1>
      <p class="lede">A factual daily review. It does not assess productivity, mood, sleep, or inactivity.</p>
    </section>
    <section class="grid" aria-label="Daily review">
      <article class="card total-card">
        <p class="card-label">Recorded activity</p>
        <p class="total">${state.total}</p>
        <p class="quiet">Aggregate duration only</p>
      </article>
      <article class="card status-card">
        <p class="card-label">Data status</p>
        <p class="status">${state.status}</p>
        <p class="quiet">${state.statusDetail}</p>
      </article>
    </section>
    <section class="card content-card" aria-labelledby="apps-title">
      <div class="section-heading"><div><p class="card-label">Ranked aggregate totals</p><h2 id="apps-title">Top apps</h2></div><span class="evidence-chip">Evidence linked</span></div>
      <div class="app-list">${list(state.apps, "No app totals are available for this date.", (app) => `<div class="app-row"><span>${app.label}</span><strong>${app.duration}</strong></div>`)}</div>
      <p class="note">When connected through a future owner-auth flow, app labels will resolve only on the owner’s device.</p>
    </section>
    <section class="card content-card" aria-labelledby="timeline-title">
      <div class="section-heading"><div><p class="card-label">Deterministic observations</p><h2 id="timeline-title">Timeline</h2></div><button class="text-button" type="button" aria-label="Timeline filters are not yet connected">Date & type filters</button></div>
      <div class="timeline-list">${list(state.timeline, "There are no aggregate observations to show for this date.", (item, index) => `<div class="timeline-row"><span class="timeline-dot" aria-hidden="true"></span><div><strong>Observation ${index + 1}</strong><p>${item}</p></div></div>`)}</div>
    </section>`;
}

picker.addEventListener("change", (event) => render(event.target.value));
render(picker.value);
