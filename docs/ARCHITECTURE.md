# Architecture

## System shape

Roxy is a local-first event pipeline with a thin AI layer.

```text
Android collectors
  -> local normalize/redact/deduplicate
  -> Room event queue
  -> WorkManager batch sync
  -> authenticated Fastify ingestion
  -> PostgreSQL immutable events
  -> deterministic facts/visits
  -> selected memories and embeddings
  -> Android/dashboard timeline and evidence-based chat
```

## Component responsibilities

### Android app

- Own permission explanations and collector toggles.
- Receive OS callbacks or query bounded incremental windows.
- Normalize and redact before persistence/upload.
- Persist before network activity.
- Batch sync under OS-managed constraints.
- Display collector health, queue state, timeline, and controls.

Collectors never know backend URLs or credentials directly. They write to one local event repository.

### API

- Authenticate a device key and apply request/rate/size limits.
- Validate versioned event envelopes.
- Insert events idempotently and return per-event acknowledgments.
- Expose structured timeline/fact/search endpoints.
- Never log sensitive payload fields.

### Database

- Keep immutable observations separate from derived facts and AI prose.
- Store provenance and algorithm/model versions.
- Enforce uniqueness and query indexes.
- Run retention and compaction as explicit, tested jobs.

### Dashboard

- Provide convenient review, diagnostics, chat, corrections, and data controls.
- Use the API only; never connect to PostgreSQL from the browser.

### AI package

- Hide provider/model details behind adapters.
- Retrieve only minimal relevant evidence.
- Treat event content as untrusted data, not instructions.
- Have no external action tools in v1.

## Dependency direction

```text
apps/api       -> packages/contracts, packages/database, packages/ai
apps/dashboard -> packages/contracts
packages/ai    -> packages/contracts (never apps)
packages/db    -> packages/contracts
apps/android   -> HTTP contract only; no TypeScript sharing
```

Packages must not import from apps. `contracts` must remain lightweight and cannot depend on database or AI packages.

## Reliability invariants

1. An accepted observation has a device-generated stable ID.
2. Retrying a batch never creates another observation.
3. Local data is not marked acknowledged until the server confirms its ID.
4. Partial failure keeps rejected events visible for diagnosis.
5. Derived facts can be deleted and rebuilt from retained observations.
6. A derived claim records evidence IDs and computation version.
7. Dates are calculated in the requested/observed timezone, not the server timezone.

## Sync outline

- Default maximum batch: 250 events or 256 KiB compressed, whichever comes first.
- Sync when connected; prefer unmetered network for large backlog.
- Exponential retry with OS-managed backoff; no busy retry loop.
- Normal target cadence: 30–60 minutes. Urgent upload is not an MVP requirement.
- Server returns `accepted`, `duplicate`, and `rejected {id, code}` lists.
- Locally quarantine permanently invalid events; surface count in Diagnostics.

The values are starting points and must be tuned with measured event volume.

## Processing outline

1. Raw normalized event is inserted.
2. A bounded job derives usage buckets, visits, and daily facts.
3. Late events mark affected local dates dirty for recomputation.
4. Daily summary is created from facts only.
5. Selected summaries/notes become searchable memories.
6. Chat routes exact questions to queries before retrieval and narration.

## Deployment stages

1. Development: Android phone -> local Wi-Fi/tunnel -> laptop API/PostgreSQL.
2. Personal alpha: private always-on host via Tailscale or equivalent private network.
3. Optional managed cloud: only after a threat review, cost measurement, and backup plan.

No free-tier promise is part of the design.

