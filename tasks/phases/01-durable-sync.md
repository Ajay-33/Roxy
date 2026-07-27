# Phase 1: Durable Sync

## Outcome

Manually created test events survive offline use, process death, reboot, retries, and partial server rejection without loss or duplication.

## Key proof

Queue 100 events offline, reconnect, retry the batch, and finish with exactly 100 server observations and zero unexplained pending events.

## Exit gate

- Versioned contract shared through fixtures.
- Device authentication and safe logging work.
- Queue diagnostics show oldest age, counts, last success, and safe error.
- Backoff creates no rapid wake/network loop.

