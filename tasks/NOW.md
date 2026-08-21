# Now: Phase 3.1 — Owner-facing UI

Phase 3 is accepted. Phase 3.1 turns its owner-authenticated aggregate data into a polished Android and web review experience.

## Phase 3.1 queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0301 | Timeline API with date/type pagination | T-0205 | Done |
| 2 | T-0302 | Android Today/Timeline shell and incomplete-data state | T-0301 | Done |
| 3 | T-0303 | Exact top-app and usage-total query endpoints | T-0301 | Done |
| 4 | T-0304 | Evidence links from fact to observations | T-0301, T-0303 | Done |
| 5 | T-0305 | Timezone and midnight regression suite | T-0301–T-0304 | Done |
| 6 | T-0306 | Android authenticated aggregate timeline reader | T-0301–T-0305 | Done |
| 7 | T-0307 | Hosted deployment and physical-phone qualification | T-0306 | Done |
| 8 | T-0310 | Android Today dashboard with local app labels and aggregate cards | T-0307 | Done |
| 9 | T-0311 | Web review dashboard foundation | T-0310 | Done |
| 10 | T-0312 | Android app-label fallback and Today-card polish | T-0311 | Done |

## Phase 3.1 outcome

- Timeline data stays aggregate-only and is isolated to the authenticated device.
- Dates/types, ordering, totals, and provenance remain deterministic.
- Empty or incompletely observed dates are visibly labelled; inactivity is never asserted as confirmed sleep.
- Owner-visible app labels are resolved locally; no labels or raw payloads enter logs, fixtures, commits, or AI prompts.

## Next unblocked task

Phase 3.1 follow-up work is complete. `tasks/active/` is intentionally empty; T-0401 is the next planned task.
