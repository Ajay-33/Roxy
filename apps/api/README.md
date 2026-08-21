# Roxy API

Reserved for the TypeScript/Fastify API. Initialize it during Phase 1 rather than installing speculative dependencies now.

Current endpoints:

- `GET /health` returns a safe service status.
- `POST /v1/sync/events` accepts authenticated, schema-validated event batches.
- `GET /v1/usage/daily` returns existing authenticated daily totals.
- `GET /v1/usage/summary` returns an exact aggregate total and deterministic top-app ranking for a requested local date.
- `GET /v1/timeline` returns a paginated, authenticated aggregate timeline for one requested local date.

## Timeline contract

`GET /v1/timeline?deviceId=<paired-device-id>&date=YYYY-MM-DD&type=usage.bucket&limit=1..100&cursor=<opaque>` requires the device Bearer credential. `type` is optional and currently accepts only `usage.bucket`; this prevents the endpoint from exposing unsupported/raw event types.

Results are ordered by UTC `occurredAt` descending, then event ID descending. The response returns payload-free metadata, an opaque `nextCursor` when another page exists, and a `completeness` object. It always reports `incomplete`: `no_aggregate_data` for an empty date and `coverage_not_proven` otherwise. Aggregate uploads do not yet provide a collector-coverage record, so the API does not infer inactivity or sleep.

## Usage summary contract

`GET /v1/usage/summary?deviceId=<paired-device-id>&date=YYYY-MM-DD&limit=1..20` requires the device Bearer credential. It returns `totalDurationMillis` as the exact sum of stored `usage.bucket` aggregate durations plus a payload-free, bounded `topApps` list. Ranking is deterministic: duration descending, then the stored app identifier ascending. The total and every ranked entry include sorted `evidenceEventIds`; resolve those IDs through the authenticated payload-free timeline endpoint. A zero result is explicitly `incomplete` with `no_aggregate_data`; a nonzero result remains `incomplete` with `coverage_not_proven` until collector coverage is recorded independently.
