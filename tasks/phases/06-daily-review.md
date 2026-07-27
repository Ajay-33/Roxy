# Phase 6: Daily Review and Data Control

## Outcome

Roxy deterministically computes and reviews each day, supports corrections, and gives the owner full data control.

## Exit gate

- Late data recomputes facts idempotently.
- Completeness is reported per day and collector.
- Corrections affect later recomputation.
- Export, range deletion, delete-all, and retention are tested.
- An encrypted backup restores successfully into a clean database.

