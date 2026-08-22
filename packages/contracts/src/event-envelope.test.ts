import assert from "node:assert/strict";
import test from "node:test";
import { eventEnvelopeV1Schema, usageSummaryQuerySchema } from "./event-envelope.ts";

const fixture = {
  id: "0195bffc-8f93-7c34-8a8f-123456789abc", schemaVersion: 1, deviceId: "synthetic-device",
  type: "system.test_event", occurredAt: "2026-08-19T00:00:00.000Z", recordedAt: "2026-08-19T00:00:01.000Z",
  timezone: "Asia/Calcutta", source: "fixture.generator", sensitivity: "private", payload: { synthetic: true },
  quality: { confidence: 1, isDerived: false },
};

test("accepts the synthetic event fixture", () => assert.equal(eventEnvelopeV1Schema.safeParse(fixture).success, true));
test("rejects an invalid fixture", () => assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, schemaVersion: 2 }).success, false));
test("accepts metadata-only notification events and rejects text", () => {
  const notification = { ...fixture, type: "notification.posted", source: "android.notification_listener", payload: { packageName: "example.synthetic", identityDigest: "a".repeat(64), redactionCount: 0 } };
  assert.equal(eventEnvelopeV1Schema.safeParse(notification).success, true);
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...notification, payload: { ...notification.payload, body: "not allowed" } }).success, false);
});
test("rejects non-v7 identifiers, invalid timezones, and invalid confidence", () => {
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, id: "0195bffc-8f93-4c34-8a8f-123456789abc" }).success, false);
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, timezone: "not/a-timezone" }).success, false);
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, quality: { confidence: 2, isDerived: false } }).success, false);
});
test("validates local calendar dates including leap-day boundaries", () => {
  assert.equal(usageSummaryQuerySchema.safeParse({ deviceId: "synthetic-device", date: "2028-02-29" }).success, true);
  assert.equal(usageSummaryQuerySchema.safeParse({ deviceId: "synthetic-device", date: "2027-02-29" }).success, false);
  assert.equal(usageSummaryQuerySchema.safeParse({ deviceId: "synthetic-device", date: "2026-13-01" }).success, false);
});
test("one UTC instant maps to the observed local date rather than a server date", () => {
  const instant = new Date("2026-08-21T20:00:00.000Z");
  const dateIn = (timeZone: string) => {
    const parts = new Intl.DateTimeFormat("en", { timeZone, year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(instant);
    const value = (type: string) => parts.find((part) => part.type === type)?.value;
    return `${value("year")}-${value("month")}-${value("day")}`;
  };
  assert.equal(dateIn("Asia/Calcutta"), "2026-08-22");
  assert.equal(dateIn("America/Los_Angeles"), "2026-08-21");
});
