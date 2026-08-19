import assert from "node:assert/strict";
import test from "node:test";
import { buildApp } from "./app.ts";
import type { EventStore, UsageTotalsStore } from "@roxy/database";

const key = "synthetic-development-key-that-is-never-a-real-device-key";
const event = { id: "0195bffc-8f93-7c34-8a8f-123456789abc", schemaVersion: 1, deviceId: "synthetic-device", type: "system.test_event", occurredAt: "2026-08-19T00:00:00.000Z", recordedAt: "2026-08-19T00:00:01.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", sensitivity: "private", payload: { synthetic: true }, quality: { confidence: 1, isDerived: false } };
const seenIds = new Set<string>();
const store: EventStore = { insert: async (storedEvent) => {
    if (seenIds.has(storedEvent.id)) return "duplicate";
    seenIds.add(storedEvent.id); return "accepted";
} };
const totalsStore: EventStore & UsageTotalsStore = { ...store, dailyAppTotals: async (deviceId, dayStart, dayEnd) => {
    assert.equal(deviceId, "synthetic-device"); assert.equal(dayStart, "2026-08-19T00:00:00.000Z"); assert.equal(dayEnd, "2026-08-20T00:00:00.000Z");
    return [{ packageName: "example.alpha", durationMillis: 1200 }, { packageName: "example.beta", durationMillis: 600 }];
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
test("returns authenticated seeded daily app totals", async () => {
    const app = buildApp(key, totalsStore); const headers = { authorization: `Bearer ${key}` };
    const response = await app.inject({ method: "GET", url: "/v1/usage/daily?deviceId=synthetic-device&dayStart=2026-08-19T00:00:00.000Z&dayEnd=2026-08-20T00:00:00.000Z", headers });
    assert.equal(response.statusCode, 200); assert.deepEqual(response.json().totals, [{ packageName: "example.alpha", durationMillis: 1200 }, { packageName: "example.beta", durationMillis: 600 }]);
    const denied = await app.inject({ method: "GET", url: "/v1/usage/daily?deviceId=x&dayStart=x&dayEnd=y" }); assert.equal(denied.statusCode, 401); await app.close();
});
