import assert from "node:assert/strict";
import test from "node:test";
import { eventEnvelopeV1Schema } from "./event-envelope.ts";

const fixture = {
  id: "0195bffc-8f93-7c34-8a8f-123456789abc", schemaVersion: 1, deviceId: "synthetic-device",
  type: "system.test_event", occurredAt: "2026-08-19T00:00:00.000Z", recordedAt: "2026-08-19T00:00:01.000Z",
  timezone: "Asia/Calcutta", source: "fixture.generator", sensitivity: "private", payload: { synthetic: true },
  quality: { confidence: 1, isDerived: false },
};

test("accepts the synthetic event fixture", () => assert.equal(eventEnvelopeV1Schema.safeParse(fixture).success, true));
test("rejects an invalid fixture", () => assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, schemaVersion: 2 }).success, false));
test("rejects non-v7 identifiers, invalid timezones, and invalid confidence", () => {
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, id: "0195bffc-8f93-4c34-8a8f-123456789abc" }).success, false);
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, timezone: "not/a-timezone" }).success, false);
  assert.equal(eventEnvelopeV1Schema.safeParse({ ...fixture, quality: { confidence: 2, isDerived: false } }).success, false);
});
