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
- `notification.updated`
- `notification.removed`
- `location.sample`
- `device.screen_state`
- `device.power_state`

Payloads are type-specific and versioned through the envelope schema plus a payload version when necessary.

### Notification lifecycle metadata

Notification activity is not a message count. A metadata-only lifecycle uses a locally derived SHA-256 digest of the package identifier plus Android's opaque notification key. The original key is never stored. The first accepted active callback is `notification.posted`; a later active callback is `notification.updated`; a removal callback is `notification.removed`. Updates and removals use the callback-observed time, so repeated Android updates are distinct even when the object's original post time is unchanged. Repeated or out-of-order callbacks at the same lifecycle time are ignored locally, and the UUIDv7 event ID makes sync retries idempotent on the server. Group summaries and missing keys are excluded.

Room migration 8→9 removes the old unique lifecycle-digest index and replaces it with a unique `(identityDigest, eventKind, occurredAtEpochMillis)` callback index, preserving existing rows while permitting lifecycle updates and removal. There is no destructive production rollback: restore a pre-migration device database backup if one is required. For disposable development data only, drop the composite index, recreate the old `identityDigest` unique index after deleting duplicate lifecycle rows, then return to schema 8. No backfill is needed because prior rows remain valid metadata events.

Notification analytics are authenticated owner-initiated reads. A requested `day` covers the requested observed local date, `week` covers that date and the preceding six observed local dates, and `month` covers the requested calendar month through that date. The response contains only activity totals, arrival/update/clear balance, hourly counts, package identifiers, and deterministic hourly burst windows (three or more activity events). It always reports incomplete coverage rather than inferring that no activity occurred outside collected data.

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
