# Data Model

This document defines conceptual records. Executable schemas and migrations will live in `packages/database` and `packages/contracts`.

## Event envelope v1

```json
{
  "id": "019...uuid-v7",
  "schemaVersion": 1,
  "deviceId": "dev_...",
  "type": "app.usage_bucket",
  "occurredAt": "2026-07-27T12:40:00.000Z",
  "recordedAt": "2026-07-27T12:45:03.000Z",
  "timezone": "Asia/Calcutta",
  "source": "android.usage_stats",
  "sensitivity": "private",
  "payload": {},
  "quality": {
    "confidence": 0.9,
    "isDerived": false
  }
}
```

## Naming

Event types use `domain.subject_action`-style stable names:

- `system.test_event`
- `app.usage_bucket`
- `notification.posted`
- `notification.removed`
- `location.sample`
- `device.screen_state`
- `device.power_state`

Payloads are type-specific and versioned through the envelope schema plus a payload version when necessary.

## Core entities

### Observation layer

- `devices`: paired collectors and revoked state.
- `events`: immutable normalized observations.
- `sync_batches`: safe operational metadata about ingestion.

### Deterministic derived layer

- `places`: user-labeled or candidate geographic areas.
- `visits`: arrival/departure windows backed by location events.
- `daily_facts`: exact computed values with evidence and algorithm version.
- `dirty_periods`: dates requiring recomputation after late/corrected events.

### Knowledge layer

- `memories`: selected prose/note units with embeddings and evidence.
- `patterns`: candidate/confirmed/dismissed statistical observations.
- `user_corrections`: append-only corrections applied during recomputation.

## Database requirements

- `events.id` is globally unique and supplied by the device.
- All mutable records have `created_at` and `updated_at` as appropriate.
- Instants are `timestamptz`; observed timezone is a separate IANA name.
- Durations use integer milliseconds or seconds with the unit in the field name.
- Coordinates are never stored as unlabeled generic JSON if they require spatial queries.
- Embedding rows include model ID and dimension.
- Evidence relations must be queryable; avoid opaque prose-only provenance.

## Retention defaults

| Record | Default |
|---|---:|
| Raw/coarse location samples | 30 days, then compact |
| Notification text | 7 days or disabled |
| Notification metadata | 90 days initially |
| App usage buckets | 1 year |
| Daily facts and corrections | Until user deletion |
| AI chat/prompt records | 30 days or disabled |
| Operational sync logs | 14 days |

Every retention deletion produces a safe count-only audit record and is tested for dependent-data behavior.

## Evolution rules

- Add fields compatibly before requiring them.
- Android may remain on an older schema during rollout.
- API rejects unknown future major versions with a clear error.
- Migrations include forward action, rollback note, and data-backfill plan.
- Re-embedding is a resumable job, never an in-place untracked rewrite.

