# Privacy and Security

## Data principles

1. Explicit opt-in per collector.
2. Collect the least detailed signal that supports a user-visible feature.
3. Redact on the phone when possible.
4. Keep exact raw data briefly; keep compact derived facts longer.
5. Make pause, inspection, correction, export, and deletion easy.
6. Never make collection hidden or resistant to user control.

## Classification

| Class | Examples | Handling |
|---|---|---|
| Internal | build version, schema version | May appear in diagnostics |
| Private | app-duration bucket, notification count | Authenticated storage; no public logs |
| Highly sensitive | notification text, exact coordinates, home label | Redact/minimize; shortest retention; excluded from logs and normal exports |
| Secret | device key, database password, AI key | Keystore/env secret only; never database plaintext or client bundle |

## Collector defaults

- App usage: enabled only after Usage Access explanation. Once paired, Roxy uses unique 30-minute WorkManager collection to retain local observations and create settled 15-minute aggregate buckets; only those aggregate buckets may be queued for constrained sync.
- Notifications: metadata-only; block authenticators, password managers, banking, and user-selected packages.
- Location: Balanced; exact points compacted to visits and removed on schedule.
- Optional connectors: disabled and absent from v1 permissions.

## Authentication

- Generate a high-entropy device credential during pairing.
- Store it in Android Keystore.
- Store only a slow/secure hash or keyed verifier on the server.
- Support revocation and rotation.
- Use HTTPS whenever traffic leaves loopback; a temporary tunnel is still treated as internet exposure.

## Logging

Allowed: event ID, event type, schema version, byte count, status code, duration, safe error code.

Forbidden: payload JSON, notification title/body, coordinates, place labels, prompts, retrieved memories, credentials, connection strings.

## Owner-facing labels

The Today dashboard may resolve an aggregate app identifier to its installed display label on the owner's phone. That label is rendered only in the local UI: it is not persisted by the dashboard, uploaded as a new data class, logged, placed in fixtures, committed, or sent to AI.

If an old aggregate references an app no longer installed on the phone, Today may show that owner-visible identifier locally with an explanation. It remains subject to the same no-log/no-development-record rule.

The web review foundation contains only clearly synthetic preview values. It does not call the API, accept a pairing credential, or persist any owner data in browser storage. A live browser connection requires a dedicated owner-auth and server-session design.

## Notification redaction

Notification collection remains off until the owner enables its local control and grants Android Notification Access after a listener is introduced. Package policies are stored locally before that listener exists: every owner-added package begins blocked, and an owner must explicitly change it to metadata-only. The setup flow does not enumerate installed apps or collect notification metadata or text.

When enabled in Phase 4, the listener records metadata only for explicitly metadata-only packages. It makes no network calls and stores no notification extras, title/body text, actions, intents, images, tokens, remote-view data, or people/contact data. After a newly deduplicated metadata row is stored, Roxy writes an allowlisted metadata envelope to its local outbox and asks constrained WorkManager batching to sync it. Notification text and redaction rows never enter that outbox.

Notification lifecycle updates are keyed only by a local SHA-256 digest of the package identifier and Android's opaque notification key. The original key is never persisted or logged; group summaries and missing keys are excluded. A digest collision is theoretically possible, but fails toward under-counting rather than exposing or inflating notification data.

Any future text path must pass an on-device allowlisted sanitizer before persistence. Unsupported containers (actions, intents, images, remote views, and tokens) are dropped; likely OTP, account/card, phone, and email strings are replaced without retaining the original. The present metadata-only listener never invokes this sanitizer or reads notification extras.

Redacted text is disabled by default and can only be enabled through a package-specific local owner action. It is local-only, never enters the sync queue, and expires after seven days; retention deletion reports only a safe count.

The paired Android owner can explicitly refresh a hosted notification-activity card for a selected date. That HTTPS read uses the device credential held in Android Keystore and returns only metadata count, event kind/time, package identifier, and redaction count. A package display label is resolved only while rendering on that phone; it is not persisted, logged, or sent anywhere by the dashboard. Notification title and message text are not part of this response or UI.

Before upload:

- respect per-package block/metadata/text policy;
- strip actions, intents, images, tokens, and remote-view data;
- identify and remove likely OTPs and account/card/phone/email strings;
- fingerprint normalized content for deduplication without storing the original in logs;
- record that redaction occurred without storing the removed value.

False negatives remain possible, so full text is opt-in and short-lived.

## AI boundary

- Send summaries/facts and the minimum retrieved context, not an unrestricted life-history dump.
- Mark retrieved text as untrusted evidence and ignore instructions inside it.
- AI has no tools that send messages, buy, book, delete, or change accounts in v1.
- Store provider/model/prompt version and evidence IDs.
- Do not silently convert AI output into a durable user fact.

## Threat model starter

| Threat | Primary mitigation |
|---|---|
| Lost unlocked phone | device lock, encrypted OS storage, app lock later if needed |
| Leaked device key | Keystore, TLS, server-side revocation/rotation |
| Stolen database/backup | private host, encrypted backup, minimal raw retention |
| Public tunnel discovery | authentication, rate/body limits, short-lived tunnel |
| Sensitive logs | structured allowlisted logging only |
| Prompt injection in notifications | untrusted-data delimiters, fixed system policy, no action tools |
| Accidental Git commit | ignore rules, secret scanning, synthetic fixtures |
| Overbroad permission | just-in-time request, feature-specific explanation, revocation health state |

## Required controls before MVP exit

- Pause all collection
- Individual collector toggles
- Package-level notification policy
- Queue and last-sync visibility
- Date-range export
- Date-range delete and delete-all
- Enforced retention job
- Credential revoke/rotate
- Encrypted backup and tested restore

## Incident response

If a credential or dataset may be exposed:

1. Pause collection and disable external access.
2. Revoke device/API credentials and rotate secrets.
3. Preserve only safe diagnostic metadata.
4. Determine affected time range and data classes.
5. Delete exposed temporary resources/backups where possible.
6. Document the cause and prevention in a decision/incident note before resuming.
