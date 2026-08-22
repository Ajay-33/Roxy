# Backlog

This is an ordered index, not a detailed task specification. Expand only the next unblocked item using `templates/TASK.md`.

## Phase 1 — Durable sync

- T-0101 Define event envelope v1 in Zod and synthetic fixtures.
- T-0102 Add Android Room event/pending-sync entities.
- T-0103 Persist a manual test event locally.
- T-0104 Add authenticated batch-ingestion endpoint.
- T-0105 Add idempotent event insert and per-ID acknowledgment.
- T-0106 Add WorkManager sync with network constraint and backoff.
- T-0107 Add queue diagnostics and offline/retry acceptance test.

## Phase 2 — App usage

- T-0201 Usage Access explainer and permission-state UI.
- T-0202 Bounded UsageStats query and normalization fixtures.
- T-0203 Incremental cursor with overlap and deduplication.
- T-0204 On-device 15-minute aggregation.
- T-0205 Server daily app totals and seeded query tests.
- T-0207 Hosted validation environment and migration runner.
- T-0208 Simplify diagnostic and pairing UI before three-day validation.
- T-0206 Three-day physical-phone accuracy/battery validation.

## Phase 3 — Timeline and exact answers

- T-0301 Timeline API with date/type pagination.
- T-0302 Android Today/Timeline shell and incomplete-data state.
- T-0303 Exact top-app and usage-total query endpoints.
- T-0304 Evidence links from fact to observations.
- T-0305 Timezone and midnight regression suite.
- T-0306 Android authenticated aggregate timeline reader.
- T-0307 Phase 3 hosted deployment and physical-phone qualification.

## Phase 3.1 — Owner-facing UI

- T-0310 Android Today dashboard with local app labels and aggregate cards.
- T-0311 Web review dashboard foundation.
- T-0312 Android app-label fallback and Today-card polish.

## Phase 4 — Notifications

- T-0401 Notification Access explainer and package policies.
- T-0402 Listener lifecycle and metadata-only events.
- T-0403 Update/group deduplication fixtures.
- T-0404 On-device sensitive-field redaction.
- T-0405 Selected-app text mode and seven-day retention.
- T-0406 Physical-phone privacy, reliability, and battery test.
- T-0407 Notification metadata contract and authenticated server persistence.
- T-0408 Android notification queue and WorkManager sync.
- T-0409 Owner-authenticated notification timeline/dashboard.
- T-0410 End-to-end phone-to-dashboard qualification and battery measurement plan.

## Phase 4.2 — Automatic collection and owner dashboard integration

The existing manual diagnostic controls are not the owner product. Replace them only after each collector has a deterministic, visible, battery-bounded background path and an authenticated owner-facing read model.

- T-0411 Automatic app-usage collection, aggregation, queueing, and bounded WorkManager sync.
- T-0412 Collector health/control model: enablement, permission/revocation, last observation, last sync, queue, failure, and battery estimates.
- T-0413 Authenticated Android dashboard: Today, usage, notification timeline/counts, completeness, and collector health from real local/server data.
- T-0414 Authenticated web dashboard integration (deferred): optional larger-screen review only; requires a dedicated owner-session design and must never reuse the device pairing credential.
- T-0415 Phone-to-hosted-server-to-dashboard qualification with automatic collection; no manual operational buttons in the normal owner flow.
- T-0416 Restore explicit notification-package metadata controls in Settings and qualify a real owner-approved package.
- T-0417 Deterministic notification metadata analytics: hourly volume, app ranking, bursts, and owner dashboard cards.

## Phase 4.3 — Notification intelligence and dashboard experience

Phase 4.2 proved the automatic phone-to-cloud pipeline. Phase 4.3 makes that existing metadata useful and legible for the owner. It must not turn notification objects or updates into claims about the number of messages, and it must not add notification content, global taps, keystroke capture, accessibility scraping, or a new collector.

- T-0418 Notification lifecycle accounting: repair notification identity/lifecycle modelling so distinct notification objects, updates, arrivals, and removals are counted separately and deterministically; preserve metadata-only handling and add database migration/rollback notes.
- T-0419 Notification analytics read model: add authenticated daily/weekly/monthly aggregates, per-app ranking, hourly distributions, update/clear balance, burst windows, and honest empty/completeness states from the repaired event model.
- T-0420 Android notification intelligence dashboard: replace the raw horizontal callback list with an owner-facing visual dashboard: summary cards, hourly bars/heatmap, source ranking bars, arrival/clear balance, date-range controls, and app drill-down. Resolve labels locally only while rendering.
- T-0421 Deterministic patterns and explanation copy: calculate transparent baselines and change signals from stored aggregates only; present plain-language observations with exact supporting counts/time ranges and no claims about message meaning, mood, productivity, or intent.
- T-0422 Dashboard information architecture and settings cleanup: separate everyday analytics from pairing, permissions, diagnostics, and developer tools; remove obsolete manual operational controls from normal owner flow and make every remaining action self-explanatory.
- T-0423 Phase 4.3 end-to-end qualification: verify lifecycle accounting, aggregate correctness, visual states, offline backlog recovery, and a battery/bytes measurement plan on the paired phone and hosted server.

## Phase 5 — Location and visits

- T-0501 Location permission sequence and Balanced/Trip controls.
- T-0502 Passive/batched samples with accuracy metadata.
- T-0503 Time-bounded Trip foreground service.
- T-0504 Synthetic visit detection and impossible-jump filtering.
- T-0505 Candidate place review and user labels.
- T-0506 Location compaction/retention job.
- T-0507 Three-day battery and route validation.

## Phase 6 — Daily review

- T-0601 Dirty-day and idempotent fact recomputation.
- T-0602 Daily completeness calculation.
- T-0603 Daily review UI and corrections.
- T-0604 Rule-based plain-language summary without AI.
- T-0605 Export and date-range/delete-all controls.
- T-0606 Encrypted backup and clean restore drill.

## Phase 7 — Memory and chat (MVP)

- T-0701 AI provider interface and disabled-by-default configuration.
- T-0702 Memory selection and embedding pipeline.
- T-0703 Hybrid retrieval with structured date filters.
- T-0704 Exact-question routing to deterministic queries.
- T-0705 Evidence-cited answer generation.
- T-0706 Prompt-injection and factual regression suite.
- T-0707 Seven-day MVP soak test.

## Post-MVP

- T-0801–T-0899 Explainable pattern engine after 2–4 weeks of history.
- T-0901–T-0999 Shadow suggestions, feedback, cooldowns, and quiet hours.
- T-1001+ Optional connectors, each with a separate privacy review.
