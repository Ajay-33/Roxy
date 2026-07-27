# Testing Strategy

## Test layers

### Unit

Pure normalization, redaction, time arithmetic, aggregation, retry classification, visit detection, and retrieval scoring.

### Contract

Android-produced JSON fixtures must validate against API schemas. Cover supported old schema versions, partial rejection, and safe error messages.

### Database integration

Run against disposable PostgreSQL: migrations, idempotent insert, recomputation, retention, deletion, and authorization boundaries.

### API integration

Exercise authentication, body limits, batch acknowledgments, structured queries, and forbidden log-field checks.

### Android instrumentation

Room migrations, WorkManager behavior, process death recovery, permission state, and Compose flows where valuable.

### Physical phone

Required for collectors and battery. Emulators cannot prove OEM background reliability, real permissions, location behavior, or energy impact.

## Essential scenarios

- 100 events queued offline and synced exactly once after reconnect.
- Same batch retried with zero duplicates.
- One bad event rejected without losing valid siblings.
- Reboot/process death preserves unacknowledged events.
- Permission revoked and re-granted without silent failure.
- Midnight and timezone changes do not double count.
- Notification updates/groups do not inflate counts.
- Sensitive notification fixtures are blocked/redacted.
- Late observations recompute affected daily facts.
- Incomplete days are labeled incomplete.
- Prompt injection stored as notification text remains inert.
- Export then date-range deletion produces the expected remaining rows.
- Backup restoration reproduces counts and constraints.

## Fixtures

Use synthetic personas and locations. Never copy real notifications, coordinates, contacts, credentials, or personal exports into tests.

## Evidence in task files

Record the command/check, result, date, and environment. If a phone test is pending, say so explicitly; never convert “not run” into “passed.”

