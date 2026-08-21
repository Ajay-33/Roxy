# Now: Phase 2 — App Usage

Phase 2 is complete as an owner-approved one-day qualification. The original three-day physical validation remains deferred follow-up work.

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
| 8 | T-0206 | One-day physical-phone qualification (three-day validation deferred) | T-0205, T-0207 | Done |

## Phase 2 exit gate

- One owner-approved day compared with Digital Wellbeing; discrepancy documented without personal usage values.
- Three-day reboot/offline/midnight/timezone validation deferred.
- Events are aggregated before upload.
- Battery measurement deferred because the current owner-triggered workflow has no continuous collector.
