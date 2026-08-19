# Now: Phase 2 — App Usage

T-0206 is in progress: three consecutive real calendar days of app-usage and battery validation. T-0207 hosted validation and T-0208 UI cleanup are complete.

## Phase 2 queue

| Order | ID | Task | Depends on | Estimate |
|---:|---|---|---|---:|
| 1 | T-0201 | Usage Access explainer and permission-state UI | Phase 1 complete | Done |
| 2 | T-0202 | Bounded UsageStats query and normalization fixtures | T-0201 | Done |
| 3 | T-0203 | Incremental cursor with overlap and deduplication | T-0202 | Done |
| 4 | T-0204 | On-device 15-minute aggregation | T-0203 | Done |
| 5 | T-0205 | Server daily app totals and seeded query tests | T-0204 | Done |
| 6 | T-0207 | Hosted validation environment and migration runner | T-0205 | Done |
| 7 | T-0208 | Simplify diagnostic and pairing UI | T-0207 | Done |
| 8 | T-0206 | Three-day physical-phone accuracy/battery validation | T-0205, T-0207 | In progress |

## Phase 2 exit gate

- Three real days compared with Digital Wellbeing; discrepancies documented.
- Reboots, offline periods, midnight, and timezone changes covered.
- Events are aggregated before upload.
- Physical-phone battery measurement is in the target or warning band with a tuning decision.
