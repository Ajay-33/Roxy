# Security Policy

Roxy is a private personal project and does not currently accept public vulnerability reports.

If a security or privacy issue is found during development:

- do not put secrets or real personal payloads in an issue;
- pause affected collection and external access;
- record a redacted `BUG-` task with severity `privacy` or `data-loss`;
- rotate exposed credentials before resuming;
- add a regression test and incident/decision note.

The complete threat model and data rules are in `docs/PRIVACY-SECURITY.md`.

