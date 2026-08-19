import assert from "node:assert/strict";
import test from "node:test";
import { buildApp } from "./app.ts";
import type { EventStore } from "@roxy/database";

const key = "synthetic-development-key-that-is-never-a-real-device-key";
const event = { id: "0195bffc-8f93-7c34-8a8f-123456789abc", schemaVersion: 1, deviceId: "synthetic-device", type: "system.test_event", occurredAt: "2026-08-19T00:00:00.000Z", recordedAt: "2026-08-19T00:00:01.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", sensitivity: "private", payload: { synthetic: true }, quality: { confidence: 1, isDerived: false } };
const seenIds = new Set<string>();
const store: EventStore = { insert: async (storedEvent) => {
    if (seenIds.has(storedEvent.id)) return "duplicate";
    seenIds.add(storedEvent.id); return "accepted";
} };

test("rejects missing credentials", async () => {
    const app = buildApp(key, store); const response = await app.inject({ method: "POST", url: "/v1/sync/events", payload: { events: [event] } });
    assert.equal(response.statusCode, 401); await app.close();
});
test("acknowledges valid events and rejects malformed ones", async () => {
    const app = buildApp(key, store); const response = await app.inject({ method: "POST", url: "/v1/sync/events", headers: { authorization: `Bearer ${key}` }, payload: { events: [event, { ...event, schemaVersion: 2 }] } });
    assert.equal(response.statusCode, 207); assert.deepEqual(response.json().acknowledgements.map((item: { id: string }) => item.id), [event.id]); assert.equal(response.json().rejected.length, 1); await app.close();
});
test("returns duplicate acknowledgement on event replay", async () => {
    const app = buildApp(key, store);
    const headers = { authorization: `Bearer ${key}` };
    await app.inject({ method: "POST", url: "/v1/sync/events", headers, payload: { events: [event] } });
    const replay = await app.inject({ method: "POST", url: "/v1/sync/events", headers, payload: { events: [event] } });
    assert.equal(replay.json().acknowledgements[0].status, "duplicate"); await app.close();
});
