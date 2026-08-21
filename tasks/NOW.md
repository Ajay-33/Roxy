# Now: Phase 3 — Timeline and exact answers

Phase 3 starts with the aggregate-only timeline read API. The deferred Phase 2 three-day physical validation remains follow-up work and is not an acceptance gate for this task.

## Phase 3 queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0301 | Timeline API with date/type pagination | T-0205 | Done |
| 2 | T-0302 | Android Today/Timeline shell and incomplete-data state | T-0301 | Done |
| 3 | T-0303 | Exact top-app and usage-total query endpoints | T-0301 | Done |
| 4 | T-0304 | Evidence links from fact to observations | T-0301, T-0303 | Done |
| 5 | T-0305 | Timezone and midnight regression suite | T-0301–T-0304 | Done |
| 6 | T-0306 | Android authenticated aggregate timeline reader | T-0301–T-0305 | Done |
| 7 | T-0307 | Hosted deployment and physical-phone qualification | T-0306 | Active |

## Phase 3 outcome

- Timeline data stays aggregate-only and is isolated to the authenticated device.
- Dates/types, ordering, totals, and provenance remain deterministic.
- Empty or incompletely observed dates are visibly labelled; inactivity is never asserted as confirmed sleep.
- Phase implementation is complete; end-to-end deployment and physical-phone qualification remain required before Phase 3 is accepted.
