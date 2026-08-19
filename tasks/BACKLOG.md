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

## Phase 4 — Notifications

- T-0401 Notification Access explainer and package policies.
- T-0402 Listener lifecycle and metadata-only events.
- T-0403 Update/group deduplication fixtures.
- T-0404 On-device sensitive-field redaction.
- T-0405 Selected-app text mode and seven-day retention.
- T-0406 Physical-phone privacy, reliability, and battery test.

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
