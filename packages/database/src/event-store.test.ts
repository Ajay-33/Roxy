import assert from "node:assert/strict";
import test from "node:test";
import { PostgresEventStore } from "./event-store.ts";

test("timeline query filters local date and uses deterministic cursor ordering", async () => {
    const store = new PostgresEventStore("postgres://synthetic:synthetic@localhost:5432/synthetic");
    const calls: Array<{ sql: string; values: unknown[] }> = [];
    const pool = (store as unknown as { pool: { query: (sql: string, values: unknown[]) => Promise<{ rows: unknown[] }> } }).pool;
    pool.query = async (sql, values) => {
        calls.push({ sql, values });
        if (sql.includes("COUNT(*)")) return { rows: [{ total: 2 }] };
        return { rows: [{ id: "synthetic-002", type: "usage.bucket", occurredAt: "2026-08-19T18:00:00.000Z", recordedAt: "2026-08-19T18:01:00.000Z", timezone: "Asia/Calcutta", source: "fixture.generator", confidence: "1", isDerived: true }] };
    };
    const page = await store.timeline({ deviceId: "synthetic-device", date: "2026-08-19", type: "usage.bucket", limit: 50, cursor: { occurredAt: "2026-08-19T18:15:00.000Z", id: "synthetic-003" } });
    assert.equal(page.total, 2); assert.equal(page.items[0]!.occurredAt, "2026-08-19T18:00:00.000Z"); assert.equal(page.items[0]!.confidence, 1);
    assert.match(calls[0]!.sql, /occurred_at AT TIME ZONE observed_timezone/); assert.match(calls[1]!.sql, /ORDER BY occurred_at DESC, id DESC/);
    assert.deepEqual(calls[1]!.values, ["synthetic-device", "2026-08-19", "usage.bucket", "2026-08-19T18:15:00.000Z", "synthetic-003", 50]);
    await store.close();
});

test("usage summary queries local-date aggregates with deterministic ranking", async () => {
    const store = new PostgresEventStore("postgres://synthetic:synthetic@localhost:5432/synthetic");
    const calls: Array<{ sql: string; values: unknown[] }> = [];
    const pool = (store as unknown as { pool: { query: (sql: string, values: unknown[]) => Promise<{ rows: unknown[] }> } }).pool;
    pool.query = async (sql, values) => {
        calls.push({ sql, values });
        if (sql.includes("COALESCE(SUM")) return { rows: [{ totalDurationMillis: "1800", evidenceEventIds: ["synthetic-001", "synthetic-002"] }] };
        return { rows: [{ appId: "synthetic.app-a", durationMillis: "900", evidenceEventIds: ["synthetic-001"] }, { appId: "synthetic.app-b", durationMillis: "900", evidenceEventIds: ["synthetic-002"] }] };
    };
    const summary = await store.usageSummary({ deviceId: "synthetic-device", date: "2026-08-19", limit: 2 });
    assert.deepEqual(summary, { totalDurationMillis: 1800, evidenceEventIds: ["synthetic-001", "synthetic-002"], topApps: [{ appId: "synthetic.app-a", durationMillis: 900, evidenceEventIds: ["synthetic-001"] }, { appId: "synthetic.app-b", durationMillis: 900, evidenceEventIds: ["synthetic-002"] }] });
    assert.match(calls[0]!.sql, /occurred_at AT TIME ZONE observed_timezone/); assert.match(calls[1]!.sql, /ORDER BY "durationMillis" DESC, "appId" ASC/);
    assert.match(calls[0]!.sql, /ARRAY_AGG\(id ORDER BY occurred_at ASC, id ASC\)/); assert.match(calls[1]!.sql, /ARRAY_AGG\(id ORDER BY occurred_at ASC, id ASC\)/);
    assert.deepEqual(calls[1]!.values, ["synthetic-device", "2026-08-19", 2]);
    await store.close();
});
