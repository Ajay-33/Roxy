import assert from "node:assert/strict";
import test from "node:test";
import { buildApp } from "./app.ts";
import type { EventStore, NotificationAnalyticsStore, NotificationSummaryStore, TimelineStore, UsageSummaryStore, UsageTotalsStore } from "@roxy/database";

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
const timelineItems = [
    { id: "synthetic-003", type: "usage.bucket", occurredAt: "2026-08-19T18:00:00.000Z", recordedAt: "2026-08-19T18:01:00.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", confidence: 1, isDerived: true },
    { id: "synthetic-002", type: "usage.bucket", occurredAt: "2026-08-19T18:00:00.000Z", recordedAt: "2026-08-19T18:01:00.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", confidence: 1, isDerived: true },
    { id: "synthetic-001", type: "usage.bucket", occurredAt: "2026-08-19T17:45:00.000Z", recordedAt: "2026-08-19T17:46:00.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", confidence: 1, isDerived: true },
];
const timelineStore: EventStore & TimelineStore = { ...store, timeline: async (query) => {
    assert.equal(query.deviceId, "synthetic-device"); assert.equal(query.date, "2026-08-19"); assert.equal(query.type, "usage.bucket");
    const afterCursor = query.cursor ? timelineItems.filter((item) => item.occurredAt < query.cursor!.occurredAt || (item.occurredAt === query.cursor!.occurredAt && item.id < query.cursor!.id)) : timelineItems;
    return { total: timelineItems.length, items: afterCursor.slice(0, query.limit) };
} };
const usageSummaryStore: EventStore & UsageSummaryStore = { ...store, usageSummary: async (query) => {
    assert.equal(query.deviceId, "synthetic-device"); assert.equal(query.date, "2026-08-19"); assert.equal(query.limit, 2);
    return { totalDurationMillis: 1_800, evidenceEventIds: ["synthetic-001", "synthetic-002"], topApps: [{ appId: "synthetic.app-a", durationMillis: 900, evidenceEventIds: ["synthetic-001"] }, { appId: "synthetic.app-b", durationMillis: 900, evidenceEventIds: ["synthetic-002"] }] };
} };
const notificationStore: EventStore & NotificationSummaryStore = { ...store, notificationSummary: async (query) => {
    assert.equal(query.deviceId, "synthetic-device"); assert.equal(query.date, "2026-08-19");
    return { count: 1, items: [{ id: "synthetic-notification", type: "notification.posted", occurredAt: "2026-08-19T10:00:00.000Z", packageName: "example.synthetic", redactionCount: 0 }] };
} };
const notificationAnalyticsStore: EventStore & NotificationAnalyticsStore = { ...store, notificationAnalytics: async (query) => {
    assert.equal(query.deviceId, "synthetic-device"); assert.equal(query.date, "2026-08-19"); assert.equal(query.period, "day");
    return { count: 3, postedCount: 1, updatedCount: 1, removedCount: 1, hourly: [{ hour: 9, count: 2 }], topApps: [{ packageName: "example.synthetic", count: 3 }], bursts: [], period: "day" };
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
test("returns an authenticated metadata-only notification summary", async () => {
    const app = buildApp(key, notificationStore); const headers = { authorization: `Bearer ${key}` };
    const response = await app.inject({ method: "GET", url: "/v1/notifications/summary?deviceId=synthetic-device&date=2026-08-19", headers });
    assert.equal(response.statusCode, 200); assert.equal(response.json().count, 1); assert.equal(response.json().items[0].body, undefined); await app.close();
});
test("returns authenticated deterministic notification analytics", async () => {
    const app = buildApp(key, notificationAnalyticsStore); const headers = { authorization: `Bearer ${key}` };
    const response = await app.inject({ method: "GET", url: "/v1/notifications/analytics?deviceId=synthetic-device&date=2026-08-19", headers });
    assert.equal(response.statusCode, 200); assert.deepEqual(response.json(), { count: 3, postedCount: 1, updatedCount: 1, removedCount: 1, hourly: [{ hour: 9, count: 2 }], topApps: [{ packageName: "example.synthetic", count: 3 }], bursts: [], period: "day", completeness: { status: "incomplete", reason: "coverage_not_proven" } });
    const denied = await app.inject({ method: "GET", url: "/v1/notifications/analytics?deviceId=synthetic-device&date=2026-08-19" }); assert.equal(denied.statusCode, 401); await app.close();
});
test("returns authenticated seeded daily app totals", async () => {
    const app = buildApp(key, totalsStore); const headers = { authorization: `Bearer ${key}` };
    const response = await app.inject({ method: "GET", url: "/v1/usage/daily?deviceId=synthetic-device&dayStart=2026-08-19T00:00:00.000Z&dayEnd=2026-08-20T00:00:00.000Z", headers });
    assert.equal(response.statusCode, 200); assert.deepEqual(response.json().totals, [{ packageName: "example.alpha", durationMillis: 1200 }, { packageName: "example.beta", durationMillis: 600 }]);
    const denied = await app.inject({ method: "GET", url: "/v1/usage/daily?deviceId=x&dayStart=x&dayEnd=y" }); assert.equal(denied.statusCode, 401); await app.close();
});
test("returns an exact deterministic aggregate usage summary", async () => {
    const app = buildApp(key, usageSummaryStore); const headers = { authorization: `Bearer ${key}` };
    const response = await app.inject({ method: "GET", url: "/v1/usage/summary?deviceId=synthetic-device&date=2026-08-19&limit=2", headers });
    assert.equal(response.statusCode, 200); assert.deepEqual(response.json(), {
        totalDurationMillis: 1_800,
        evidenceEventIds: ["synthetic-001", "synthetic-002"],
        topApps: [{ appId: "synthetic.app-a", durationMillis: 900, evidenceEventIds: ["synthetic-001"] }, { appId: "synthetic.app-b", durationMillis: 900, evidenceEventIds: ["synthetic-002"] }],
        completeness: { status: "incomplete", reason: "coverage_not_proven" },
    }); await app.close();
});
test("returns a safe empty usage summary and rejects invalid summary input", async () => {
    const emptyStore: EventStore & UsageSummaryStore = { ...store, usageSummary: async () => ({ totalDurationMillis: 0, evidenceEventIds: [], topApps: [] }) };
    const app = buildApp(key, emptyStore); const headers = { authorization: `Bearer ${key}` };
    const empty = await app.inject({ method: "GET", url: "/v1/usage/summary?deviceId=synthetic-device&date=2026-08-19", headers });
    const invalidDate = await app.inject({ method: "GET", url: "/v1/usage/summary?deviceId=synthetic-device&date=2026-02-30", headers });
    const invalidLimit = await app.inject({ method: "GET", url: "/v1/usage/summary?deviceId=synthetic-device&date=2026-08-19&limit=21", headers });
    const denied = await app.inject({ method: "GET", url: "/v1/usage/summary?deviceId=synthetic-device&date=2026-08-19" });
    assert.deepEqual(empty.json(), { totalDurationMillis: 0, evidenceEventIds: [], topApps: [], completeness: { status: "incomplete", reason: "no_aggregate_data" } });
    assert.equal(invalidDate.statusCode, 400); assert.equal(invalidLimit.statusCode, 400); assert.equal(denied.statusCode, 401); await app.close();
});
test("returns a deterministic aggregate-only timeline with cursor pagination", async () => {
    const app = buildApp(key, timelineStore); const headers = { authorization: `Bearer ${key}` };
    const first = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-08-19&type=usage.bucket&limit=2", headers });
    assert.equal(first.statusCode, 200); assert.deepEqual(first.json().items.map((item: { id: string }) => item.id), ["synthetic-003", "synthetic-002"]);
    assert.equal(first.json().items[0].payload, undefined); assert.equal(first.json().completeness.status, "incomplete"); assert.equal(first.json().completeness.reason, "coverage_not_proven");
    const second = await app.inject({ method: "GET", url: `/v1/timeline?deviceId=synthetic-device&date=2026-08-19&type=usage.bucket&limit=2&cursor=${encodeURIComponent(first.json().nextCursor)}`, headers });
    assert.deepEqual(second.json().items.map((item: { id: string }) => item.id), ["synthetic-001"]); assert.equal(second.json().nextCursor, null); await app.close();
});
test("labels an empty timeline incomplete and rejects invalid timeline input", async () => {
    const emptyStore: EventStore & TimelineStore = { ...store, timeline: async () => ({ total: 0, items: [] }) };
    const app = buildApp(key, emptyStore); const headers = { authorization: `Bearer ${key}` };
    const empty = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-08-19", headers });
    assert.deepEqual(empty.json().completeness, { status: "incomplete", reason: "no_aggregate_data" });
    const invalidDate = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=not-a-date", headers });
    const impossibleDate = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-02-30", headers });
    const invalidType = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-08-19&type=system.test_event", headers });
    const malformedCursor = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-08-19&cursor=not-a-cursor", headers });
    const denied = await app.inject({ method: "GET", url: "/v1/timeline?deviceId=synthetic-device&date=2026-08-19" });
    assert.equal(invalidDate.statusCode, 400); assert.equal(impossibleDate.statusCode, 400); assert.equal(invalidType.statusCode, 400); assert.equal(malformedCursor.statusCode, 400); assert.equal(denied.statusCode, 401); await app.close();
});
