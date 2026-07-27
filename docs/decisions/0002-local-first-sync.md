# ADR-0002: Local-first event collection

- Status: accepted
- Date: 2026-07-27

## Context

The phone will be offline, the API will fail, and Android may defer background work. Direct collector uploads would couple sensing to connectivity and risk loss, duplicates, wakeups, and excess battery drain.

## Decision

Every collector writes a normalized event to Room. WorkManager sends bounded idempotent batches. Local rows remain pending until individually acknowledged.

## Consequences

- Collection works without network access.
- Retries and partial failures need explicit states and tests.
- The queue and its oldest item must be visible in Diagnostics.
- Batched networking improves battery behavior.

