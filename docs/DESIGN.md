# Experience Design

## Design goals

Roxy should feel calm, factual, private, and easy to correct. It is a personal journal with sensors—not a surveillance dashboard.

## Navigation

Planned Android tabs:

1. **Today** — concise day view and data completeness.
2. **Timeline** — chronological evidence with filters.
3. **Ask** — questions and cited answers.
4. **Control** — collectors, privacy, battery, export/delete.

Diagnostics lives inside Control but remains reachable when collection is failing.

## Key screens

### Onboarding

- Explain value before requesting any permission.
- Request one permission only when its collector is enabled.
- Show a plain example of captured and excluded data.
- Default notifications to metadata-only and location to Balanced.
- Allow skipping every collector.

### Today

- Data completeness banner: complete, partial, delayed, or paused.
- Compact daily facts with source links.
- Timeline preview.
- “Add note” and “Correct” actions.
- No guilt language, productivity score, or invented mood.

### Collector control

Each collector card shows:

- on/off state and OS permission state;
- what it collects and never collects;
- last successful observation;
- approximate events and uploaded bytes in 24 hours;
- measured battery contribution when available;
- retention setting and delete action.

### Ask

- Suggest factual starter questions.
- Render citations as tappable evidence chips.
- Clearly label calculation, observation, inference, and missing data.
- Feedback actions: correct, missing context, not useful.

## Visual direction

- Neutral dark/light system theme with one restrained accent color.
- Use generous spacing and readable typography rather than dense charts.
- Never use alarming red for ordinary missing data; reserve it for security or data-loss risk.
- Charts appear only when they reveal a trend better than a sentence.
- Location maps obscure saved “home” coordinates in screenshots/exports by default.

## Accessibility

- Support system font scaling and screen-reader labels.
- Do not encode collector state or confidence by color alone.
- Tap targets at least 48dp.
- Provide exact text equivalents for charts.

## Notification policy

- Collection-status notification is functional and honest.
- Product suggestions are off until Phase 9.
- Initial cap: one suggestion/day, with quiet hours and immediate disable.
- Never send sensitive content to the lock screen by default.

