# Personal Context Engine — Realistic Build Plan

> Status: planning only  
> Last revised: 2026-07-27  
> Scope: one owner, one Android phone, sideloaded app, no public release, no scaling work

---

## 1. Product definition

The Personal Context Engine (PCE) is a private, local-first system that builds a useful history of my days from signals my Android phone can legally and reliably expose. It should answer factual questions, find routines, produce daily/weekly reviews, and eventually offer a small number of explainable suggestions.

It is **not** an omniscient agent. A normal, non-root Android app cannot see every screen, private app database, encrypted conversation, browser-incognito history, exact human intent, or everything done away from the phone. It can observe only granted signals and user-provided information.

### Initial outcomes

After the MVP, I should be able to ask:

- “Where did I spend time yesterday?”
- “Which apps used most of my evening?”
- “What important notifications did I receive?”
- “Summarize my day, and show the evidence behind each statement.”
- “How has my commute or bedtime proxy changed this month?”

### Explicit non-goals for v1

- Reading full content inside arbitrary apps
- Recording calls, microphone, camera, keystrokes, or screen continuously
- Accessibility-service scraping or VPN traffic interception
- Rooting the phone
- Autonomous purchases, messages, bookings, or account changes
- Diagnosing health, mood, relationships, or finances from weak signals
- Multi-user support, Play Store compliance, or production-scale infrastructure

These exclusions reduce battery drain, security exposure, fragile hacks, and misleading AI conclusions.

---

## 2. What can actually be collected

| Signal | Feasibility | Collection method | Important limitation | v1 policy |
|---|---|---|---|---|
| App usage | Good | `UsageStatsManager` with Usage Access | Approximate aggregates; no in-app content | Collect package, start/end bucket, duration |
| Notifications | Good | `NotificationListenerService` with user-granted access | Only notification-visible text; apps may redact it | Off by default per app; redact sensitive fields |
| Location | Good but battery-sensitive | Fused Location Provider, geofences, activity-aware batches | Background delivery is throttled; continuous mode needs a visible notification | Store visits and coarse paths, not high-rate GPS |
| Device state | Good | Battery, charging, screen interactive/unlock, network type | Some broadcasts are restricted | Store state transitions only |
| Calendar | Good if explicitly connected/imported | Android calendar provider or later connector | Highly sensitive; separate permission | Later opt-in module |
| Steps/activity | Device-dependent | Health Connect or Activity Recognition | User/device support varies | Later opt-in module |
| Spending | Partial | Notification parsing, email import, or manual CSV | Notifications are incomplete and parsing can be wrong | Later; every transaction must retain source/confidence |
| Contacts/calls/SMS | Sensitive and restricted | Platform providers with permissions | Store/policy restrictions and privacy risk | Excluded from v1 |
| Screen contents/keystrokes | Technically hacky, unsafe | Accessibility or capture APIs | Fragile, invasive, visible consent requirements | Do not collect |

### Core principle

“Everything possible” means **everything explicitly enabled, technically reliable, and useful enough to justify its privacy and battery cost**. Every collector must have its own toggle, explanation, retention policy, health status, and delete action.

---

## 3. Technical decisions

### Chosen stack

| Layer | Choice | Why |
|---|---|---|
| Android app | Native Kotlin + Jetpack Compose | The difficult work is Android services, permissions, Room, and WorkManager; avoids a React Native bridge |
| Phone database | Room (SQLite) | Reliable offline queue and local timeline |
| Local secrets | Android Keystore | Keeps device credentials out of source and preferences |
| Scheduled work | WorkManager | OS-supported retries, constraints, and periodic sync |
| Location | Fused Location Provider + geofencing | Better battery behavior than fixed GPS polling |
| Backend | TypeScript + Fastify + Zod | Small, typed API with runtime validation |
| Database | PostgreSQL + pgvector | Structured analytics and semantic retrieval in one database |
| DB access | Drizzle ORM + SQL migrations | Typed code while keeping vector/SQL operations straightforward |
| Background jobs | Same backend process initially, invoked by a scheduler | Avoids adding a queue system before it is needed |
| Dashboard | Next.js + TypeScript | Timeline, settings, diagnostics, and chat |
| AI provider | Provider adapter; Gemini first | Prevents model IDs and provider details spreading through the codebase |
| Embeddings | `gemini-embedding-2`, configurable dimension (start at 768) | Current stable Gemini embedding option; model and dimension stay in config |
| Repository | pnpm workspace + Gradle Android project | One repo, shared TypeScript contracts where applicable |

### Deployment decision

Start **local-first and laptop-hosted** during development:

- Android app stores events locally.
- Backend and PostgreSQL run on the development computer with Docker Compose.
- The phone reaches the backend over local Wi-Fi or a temporary HTTPS tunnel.
- Nothing depends on a free hosting plan during early development.

After one week of reliable collection, choose one:

1. **Private home deployment (preferred):** small always-on computer + Tailscale, with encrypted backups.
2. **Managed cloud:** hosted PostgreSQL and backend, with raw notification text disabled or client-side encrypted.

Free-tier quotas and prices change, so they are not architectural assumptions.

---

## 4. Architecture

```text
ANDROID PHONE
  Collectors (independently enabled)
    ├─ app-usage snapshots
    ├─ notification events
    ├─ location samples / visit transitions
    └─ device-state transitions
             ↓
  Normalize → redact → deduplicate
             ↓
  Room database (source of truth until acknowledged)
             ↓ WorkManager: Wi-Fi/battery-aware HTTPS batches

BACKEND
  Validate → authenticate device → idempotent insert
             ↓
  PostgreSQL
    ├─ immutable normalized events
    ├─ derived visits and usage sessions
    ├─ daily summaries and facts
    ├─ memories + embeddings
    └─ patterns + evidence
             ↓
  Deterministic analytics first → AI narration/retrieval second

CLIENTS
  Android app and web dashboard
    ├─ today / timeline
    ├─ data and collector health
    ├─ search / chat with citations
    └─ review, correct, export, delete
```

### Data flow rules

1. Collectors never call the network directly.
2. Every raw observation is normalized locally into a versioned event envelope.
3. Every event has a stable device-generated ID; retries cannot create duplicates.
4. Server acknowledgment is recorded before local queue cleanup.
5. Deterministic code computes totals, durations, visits, and rankings.
6. AI summarizes computed facts and retrieved evidence; it does not invent analytics from a raw event dump.
7. Every insight stores its evidence IDs, confidence, generation version, and timestamp.

---

## 5. Repository structure (to create only when implementation starts)

```text
personal-context-engine/
├─ apps/
│  ├─ android/                 # Kotlin + Compose + Room + WorkManager
│  ├─ api/                     # Fastify HTTP API and scheduled jobs
│  └─ dashboard/               # Next.js web UI
├─ packages/
│  ├─ contracts/               # Zod API/event schemas
│  ├─ database/                # Drizzle schema, migrations, queries
│  └─ ai/                      # provider interface, prompts, retrieval
├─ infra/
│  ├─ docker-compose.yml       # local PostgreSQL/pgvector
│  └─ backup/                  # documented backup/restore scripts
├─ docs/
│  ├─ architecture.md
│  ├─ privacy-model.md
│  ├─ android-permissions.md
│  └─ decisions/               # short architecture decision records
├─ .env.example
├─ .gitignore
└─ README.md
```

Do not share code between Kotlin and TypeScript prematurely. The event schema is the contract; generate examples and contract tests instead.

---

## 6. Event and database model

### Canonical event envelope

```json
{
  "id": "uuid-v7-created-on-device",
  "schemaVersion": 1,
  "deviceId": "random-device-id",
  "type": "app.usage_bucket",
  "occurredAt": "2026-07-27T18:10:00+05:30",
  "recordedAt": "2026-07-27T18:15:03+05:30",
  "timezone": "Asia/Calcutta",
  "source": "android.usage_stats",
  "sensitivity": "private",
  "payload": {},
  "quality": {
    "confidence": 0.9,
    "isDerived": false
  }
}
```

Always store UTC instants plus the original timezone. Never use a single ambiguous `timestamp` field.

### Essential tables

#### `devices`

- `id`, `name`, `api_key_hash`, `created_at`, `last_seen_at`, `revoked_at`

#### `events`

- `id` UUID primary key supplied by device
- `device_id`, `schema_version`, `event_type`
- `occurred_at`, `recorded_at`, `timezone`
- `source`, `sensitivity`, `payload` JSONB, `quality` JSONB
- `ingested_at`
- indexes on `(device_id, occurred_at)`, `(event_type, occurred_at)`

#### `sync_batches`

- `id`, `device_id`, `received_at`, `event_count`, `accepted_count`, `rejected_count`, `error_summary`

#### `places`

- `id`, `label`, coarse center/geohash, radius, `source`, `confidence`, timestamps
- Home and other sensitive labels must be hidden in logs and exports by default.

#### `visits`

- `id`, `place_id`, arrival/departure, duration, confidence, supporting event IDs

#### `daily_facts`

- `local_date`, `fact_type`, structured `value`, `evidence_event_ids`, algorithm version
- Examples: app totals, first/last phone activity, time by place, notification count

#### `memories`

- `id`, `kind` (`daily_summary`, `user_note`, `important_event`)
- `content`, `embedding`, `embedding_model`, `embedding_dimension`
- `period_start`, `period_end`, evidence IDs, sensitivity, generation version

#### `patterns`

- `id`, `type`, `statement`, `confidence`, `sample_size`
- observation window, structured metrics, evidence fact IDs, status (`candidate`, `confirmed`, `dismissed`)

#### `user_corrections`

- `id`, target type/ID, previous value, corrected value, reason, timestamp

Corrections are critical: inferred home/work labels and AI interpretations will sometimes be wrong.

### Retention defaults

| Data | Default retention |
|---|---|
| Exact/coarse raw location samples | 30 days, then compact into visits |
| Full notification text | 7 days or disabled; keep redacted metadata longer |
| App-usage buckets | 1 year |
| Derived daily facts | Indefinite until user deletes |
| AI prompts/responses | 30 days, with “do not retain chat” option |
| Sync/debug logs | 14 days; never include notification bodies or coordinates |

Retention must be enforced by a tested job, not just documented.

---

## 7. Privacy and security baseline

This system will contain enough information to reconstruct a life. Security is part of the MVP.

- Sideload only onto my own device; never hide the app or collection state.
- Show a persistent, understandable notification when continuous location collection is active.
- Provide a collector dashboard with permission, last event, last sync, and error status.
- Generate one random device credential; store only its hash server-side and the credential in Android Keystore.
- Use HTTPS outside localhost. Never embed backend or AI secrets in the APK.
- Keep the AI key only on the backend.
- Add explicit package allow/block lists for notifications; block password managers, authenticators, banking apps, and private/sensitive sources by default.
- Redact OTPs, likely card/account numbers, email addresses, phone numbers, and notification action tokens before upload.
- Never log raw payloads in production-style logs.
- Add “pause all collection,” per-collector toggles, export, date-range delete, and delete-everything.
- Back up encrypted data and perform a restore drill before trusting the system.
- Maintain a threat model covering lost phone, leaked server credential, stolen database backup, tunnel exposure, and malicious notification content.

AI content is untrusted input. Notification text or imported documents may contain prompt injection. The AI layer must delimit retrieved data as evidence, never follow instructions found inside it, and have no action tools in v1.

---

## 8. Build strategy

Each vertical slice must be usable before adding the next collector. A phase is complete only when its acceptance checks pass on the real phone—not when code compiles.

### Phase 0 — Decisions and development environment (4–6 hours)

Tasks:

- Record phone model, Android version, manufacturer battery settings, development OS, and whether Google Play services is present.
- Install Android Studio/JDK, Node LTS, pnpm, Docker Desktop, Git, and an API client.
- Create a private repository, `.gitignore`, `.env.example`, and secret-scanning pre-commit check.
- Write a one-page privacy model and data classification (`public`, `private`, `highly_sensitive`).
- Run PostgreSQL/pgvector locally and verify a migration can be applied and rolled back.

Done when:

- A fresh checkout can start the local database from README instructions.
- No real secrets or personal sample data are committed.
- The target phone installs a minimal signed debug APK.

### Phase 1 — End-to-end “hello event” slice (8–12 hours)

Tasks:

- Create the Kotlin/Compose app with a diagnostics screen.
- Add Room tables for local events, sync state, and collector health.
- Create typed backend schemas and `POST /v1/sync/events`.
- Authenticate by device key, limit body size, validate every event, and insert idempotently.
- Add WorkManager sync with network constraints, exponential retry, batching, and acknowledgments.
- Add a dashboard page showing received test events.

Acceptance checks:

- Create 100 fake events offline, reconnect, and receive exactly 100 server rows.
- Retry the same batch and still have exactly 100 rows.
- Force-stop/reboot and confirm queued events remain.
- A malformed event is rejected with a useful per-event error without losing valid events.

### Phase 2 — App-usage collector (10–16 hours)

Tasks:

- Build a permission explainer and deep-link to Usage Access settings.
- Query incremental usage windows; persist a cursor with overlap to tolerate delayed system data.
- Normalize package names and cache human-readable app labels locally.
- Aggregate into 5- or 15-minute buckets before upload.
- Add daily totals and usage-session reconstruction on the server.

Acceptance checks:

- Compare three days against Digital Wellbeing; document expected discrepancy.
- Changing timezone and crossing midnight does not double-count usage.
- Revoking permission produces a visible health warning, not silent failure.
- Collection works after reboot and after a day offline.

### Phase 3 — Timeline and deterministic answers (10–14 hours)

Tasks:

- Build Today and Timeline views with date/type filters.
- Add SQL endpoints for app totals, hourly usage, and recent events.
- Implement the first “chat-like” questions through intent routing to deterministic queries:
  - most-used apps for a date/range
  - total screen/app activity
  - first/last observed activity
- Display result provenance and clarify that phone inactivity is only a sleep proxy.

Acceptance checks:

- Known seeded data returns exact totals.
- Every answer links to its source facts/events.
- Empty and partially synced days are labeled incomplete.

This is the first useful checkpoint and should be completed before notification or location access.

### Phase 4 — Notification collector with redaction (12–20 hours)

Tasks:

- Implement `NotificationListenerService`; move callback work off the main thread.
- Capture posted/removed time, package, category, stable fingerprint, and allowed visible fields.
- Deduplicate notification updates and grouped notification summaries.
- Build per-app `metadata only / redacted text / blocked` controls.
- Apply on-device redaction and discard configured OTP messages after extracting no content.

Acceptance checks:

- Test normal, updated, grouped, ongoing, and removed notifications.
- Banking/password-manager/authenticator test notifications never leave the phone.
- Duplicate callbacks do not inflate counts.
- Permission removal/disconnection is visible in diagnostics.

### Phase 5 — Battery-aware location and visits (14–24 hours)

Tasks:

- Request foreground location first and background location later through separate explanations.
- Implement two modes:
  - **Balanced (default):** passive/batched updates, significant movement, geofences.
  - **Trip mode:** foreground location service with persistent notification and higher detail.
- Store accuracy and provider with every sample; reject impossible jumps.
- Cluster samples into candidate places and visits; user labels home/work/gym manually.
- Compact old samples according to retention policy.

Acceptance checks:

- Test stationary home, commute, short stop, poor GPS, offline day, reboot, and timezone change.
- Visits are reasonably correct without pretending to be exact.
- Measure a 24-hour battery comparison; default mode target is under roughly 5% additional drain, then tune for the actual phone.
- Location stops immediately when its toggle is disabled.

### Phase 6 — Daily facts and review (10–16 hours)

Tasks:

- Compute a daily fact sheet using SQL/code: usage, visits, notification counts, active-time proxies, data completeness.
- Make jobs idempotent and re-runnable after late events.
- Build a daily review UI where I can correct place labels and mark facts as important/wrong.
- Generate a plain-language daily summary from the fact sheet, requiring evidence references.

Acceptance checks:

- Re-running a day produces the same facts unless source data changed.
- AI summary numbers match deterministic facts.
- Unsupported claims are rejected or labeled as hypotheses.
- The UI clearly shows which collectors were incomplete.

### Phase 7 — Searchable memory and evidence-based chat (14–22 hours)

Tasks:

- Create memories only from daily summaries, explicit notes, and selected important events—not every raw event.
- Embed memories through a provider adapter; store model and dimension beside each vector.
- Implement hybrid retrieval: structured date filters + full-text/vector search + recency weighting.
- Build `/v1/chat` with a fixed pipeline:
  1. parse time range and question type;
  2. call deterministic tools for numbers;
  3. retrieve relevant memories/evidence;
  4. answer with citations and uncertainty;
  5. store feedback, not hidden “facts.”
- Add a small regression dataset of questions and expected answers.

Acceptance checks:

- Date arithmetic is correct in `Asia/Calcutta` and across timezone changes.
- Numerical questions match SQL, not model estimates.
- Each factual claim has evidence; missing data is admitted.
- Prompt-injection text inside a notification cannot alter system behavior.
- Re-embedding can occur as a resumable migration if the model changes.

**MVP is complete here.** Run it for at least two weeks before adding proactive behavior.

### Phase 8 — Pattern engine (16–24 hours after 2–4 weeks of data)

Start with explainable statistics, not free-form AI discovery.

Candidate detectors:

- recurring place visits and commute windows
- weekday/weekend app-usage distribution
- evening phone-use trend
- notification volume by app and hour
- approximate first/last activity window

Each pattern requires minimum sample size, confidence, evidence, freshness, and a user confirmation/dismissal state. Avoid causal or emotional statements.

Acceptance checks:

- Run detectors on synthetic histories with known patterns.
- A missing-data week lowers confidence.
- The UI explains “why I think this” and lets me correct/dismiss it.

### Phase 9 — Suggestions in shadow mode (12–20 hours)

Tasks:

- Define suggestion rules with cooldowns, quiet hours, relevance score, expiry, and daily cap.
- Run for one week in **shadow mode**: log proposed suggestions without notifying.
- Review false positives and add feedback (`useful`, `not useful`, `wrong`, `too frequent`).
- Enable at most one low-risk suggestion per day initially.

Examples:

- “Your evening social-app use is 35 minutes above your recent weekday median.”
- “You usually leave for this recurring place around 08:40; today’s calendar starts earlier.” (only if calendar is later enabled)

No medical, financial, relationship, or safety-critical advice. No autonomous action.

### Phase 10 — Optional connectors, one at a time

Possible later modules:

- manual notes and voice notes initiated by the user
- calendar read-only import
- Health Connect steps/sleep if available
- financial CSV/email import with a dedicated parser and reconciliation UI
- photos metadata (not image contents by default)
- browser history from an explicit browser export/extension

Every connector repeats the same mini-process: threat review → separate consent → local prototype → normalized schema → health UI → retention/delete → test week.

---

## 9. Realistic schedule

AI-assisted coding accelerates scaffolding, tests, and UI, but device permissions, OEM battery behavior, and real-world validation still take elapsed time.

| Checkpoint | Focused build time | Real-world validation |
|---|---:|---:|
| Phases 0–1 | 12–18 h | 1–2 days |
| Phases 2–3 | 20–30 h | 3 days |
| Phase 4 | 12–20 h | 2–3 days |
| Phase 5 | 14–24 h | 3–5 days |
| Phases 6–7 | 24–38 h | 1 week |
| MVP total | **82–130 h** | **about 3–6 calendar weeks** |
| Phases 8–9 | 28–44 h | requires 2–4 weeks of history |

Do not estimate cloud storage until measuring actual event volume and redaction. A personal system can still create millions of noisy rows if events are not aggregated.

---

## 10. Testing and observability

### Required automated tests

- Event schema compatibility and invalid payloads
- Idempotent sync and partial batch rejection
- Usage aggregation around midnight/timezone changes
- Notification deduplication and redaction fixtures
- Visit detection with synthetic paths
- Daily-fact recomputation
- Retention deletion and export
- Retrieval/chat regression cases
- Authorization: wrong/revoked key and cross-device isolation

### Phone test matrix

- Wi-Fi/mobile/no network, low battery, battery saver
- app backgrounded, swiped away, force-stopped, rebooted
- permission granted/revoked/re-granted
- clock/timezone changed
- several hours stationary and a real commute
- server unavailable for 24 hours
- database restored from backup

### Diagnostics screen

Show without exposing sensitive payloads:

- app/build/schema version
- each permission and collector state
- last event per collector
- queued event count and oldest queued age
- last successful sync and last safe error message
- 24-hour event counts and approximate battery impact
- backend health and clock skew

Silent failure is the main enemy of a passive system; diagnostics are a feature, not cleanup work.

---

## 11. Vibe-coding operating rules

Work in very small tickets—normally one behavior plus tests. Never ask an AI agent to “build Phase 5” in one prompt.

### Ticket template

```text
Goal:
One observable behavior to add.

Relevant files:
List the exact files/modules after inspecting the repository.

Inputs and outputs:
Give schemas and one example.

Constraints:
- Android/API versions involved
- privacy and battery rules
- offline/idempotency behavior
- no unrelated refactors

Acceptance checks:
1. Automated test ...
2. Manual phone test ...
3. Failure state visible in diagnostics ...

Before coding:
Explain the proposed change and risky assumptions.

After coding:
Run focused tests, summarize changed files, and give the exact manual test.
```

### Rules for AI-generated code

- Inspect current official documentation when permissions/background behavior is involved.
- Compile after each native Android change; test on the physical target phone early.
- Require migrations for schema changes; never manually mutate the main database.
- Require tests for normalization, redaction, deduplication, time calculations, and destructive deletion.
- Do not paste real personal data into coding chats, issues, fixtures, or logs.
- Commit at every passing vertical slice with a short rollback-friendly change.
- Keep an architecture decision record when changing stack, storage, encryption, or collection policy.

---

## 12. Decisions to record before implementation

Fill these in when work begins:

- Target phone model/manufacturer: **iQOO Z7 Pro 5G (I2301)**
- Android version/API level: **Android 15 / API 35**
- Google Play services available: **Yes**
- Development machine can run Docker continuously: **Yes; Docker Engine 28.0.4 verified.**
- First deployment: **Managed cloud later; local development environment first**
- Notification default: **Metadata only**
- Location default: **Balanced. Trip mode is user-started, visibly active, and automatically stops after 4 hours.**
- Raw location retention: **30 days**
- Raw notification-text retention: **7 days**
- AI provider and model: **choose from current stable models at implementation time**
- Monthly budget ceiling: **₹0; do not introduce paid services or plans.**

---

## 13. Definition of MVP success

The MVP succeeds when all of the following are true for seven consecutive days:

- App usage, selected notifications, and balanced location collect with no unexplained multi-hour gaps.
- Offline events sync later without duplication or loss.
- Battery impact is acceptable on the actual phone.
- The daily timeline and computed totals are accurate enough to trust.
- Chat answers the agreed regression questions with evidence and admits missing data.
- Collection can be paused, inspected, exported, and deleted.
- A database backup has been restored successfully.
- No secret or raw sensitive payload appears in repository history or application logs.

Only after this should the project learn patterns or send proactive suggestions.

---

## 14. Build order

```text
Environment and privacy decisions
  → durable offline sync
  → app usage
  → deterministic timeline/answers
  → carefully filtered notifications
  → battery-aware location/visits
  → daily facts and review
  → evidence-based AI memory/chat (MVP)
  → observe for 2–4 weeks
  → explainable patterns
  → shadow suggestions
  → optional connectors one by one
```

The system should earn access to more personal data by first proving reliability, usefulness, and control with less data.
