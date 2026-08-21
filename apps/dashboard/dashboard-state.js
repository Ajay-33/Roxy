export const previewStates = Object.freeze({
  preview: Object.freeze({
    title: "Today",
    eyebrow: "Preview data — not from your phone",
    total: "2 h 15 min",
    status: "Partial data",
    statusDetail: "Coverage has not yet been independently verified for this date.",
    apps: Object.freeze([
      Object.freeze({ label: "Sample app A", duration: "1 h 25 min" }),
      Object.freeze({ label: "Sample app B", duration: "50 min" }),
    ]),
    timeline: Object.freeze([
      "Aggregate activity observation · evidence available",
      "Aggregate activity observation · evidence available",
      "Aggregate activity observation · evidence available",
    ]),
  }),
  empty: Object.freeze({
    title: "No aggregate data for this date",
    eyebrow: "Empty date",
    total: "—",
    status: "No data recorded",
    statusDetail: "Roxy cannot distinguish an empty record from inactivity without verified coverage.",
    apps: Object.freeze([]),
    timeline: Object.freeze([]),
  }),
  unavailable: Object.freeze({
    title: "Review data is unavailable",
    eyebrow: "Connection required",
    total: "—",
    status: "Not connected",
    statusDetail: "This browser preview never stores a pairing credential. Connect through a future owner-auth flow.",
    apps: Object.freeze([]),
    timeline: Object.freeze([]),
  }),
});

export function stateFor(name) {
  return previewStates[name] ?? previewStates.preview;
}
