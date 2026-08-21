# Roxy Dashboard

An owner-facing review-dashboard foundation. It is intentionally static: no
credential, personal data, app identifier, or live API request is included in
the browser bundle. The available preview states exercise the Today, Timeline,
evidence, and incomplete-data presentation.

Run `pnpm --filter @roxy/dashboard start` and open the local address printed by
the server. Use the state switcher to inspect the normal preview, empty date,
and unavailable-data state.

## Future integration boundary

The current API uses the paired-device credential. A browser must not embed or
persist that credential. Connecting a real dashboard therefore requires a
separate owner-auth design, server-side session boundary, and privacy review.
That work is intentionally outside this foundation.
