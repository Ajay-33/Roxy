# T-0414: Authenticated web dashboard integration

- Status: deferred
- Phase: 4.2 / Automatic collection and owner dashboard integration
- Estimate: 1–4 hours
- Depends on: T-0413

## Goal

Replace the synthetic web review shell with a real, read-only owner dashboard behind a dedicated server-session authentication boundary. The Android pairing credential must never enter browser code or storage.

## Read first

- `AGENTS.md`
- `docs/PRIVACY-SECURITY.md`
- `apps/web/`
- `apps/api/`
- `packages/contracts/`

## In scope

- Dedicated server-side owner session authentication and logout/expiry behavior.
- Read-only web dashboard for real aggregate usage, notification metadata counts/timeline, and safe collector/sync status.
- Explicit empty, unavailable, unauthenticated, and incomplete states.
- Synthetic tests only; no owner data in fixtures/logs.

## Out of scope

- Reusing/storing device pairing credentials in a browser, text upload, action-taking web controls, or new collection.

## Acceptance checks

- [ ] Automated: protected web read APIs reject unauthenticated sessions; server/web checks pass.
- [ ] Manual: owner can sign in and view only their real aggregate dashboard, then log out.
- [ ] Privacy: no pairing secret/browser storage; no notification text or raw payload reaches the browser.

## Deferral note

Deferred on 2026-08-22: Roxy’s Android app is the owner product and already has the required dashboard. There is no existing web app or dedicated owner-authentication secret/session design; this optional surface remains deferred until a larger-screen review need justifies that privacy boundary.
