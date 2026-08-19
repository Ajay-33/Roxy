import { Pool } from "pg";
import type { EventEnvelopeV1 } from "@roxy/contracts";

export type InsertResult = "accepted" | "duplicate";
export interface EventStore { insert(event: EventEnvelopeV1): Promise<InsertResult>; close?(): Promise<void>; }
export type DailyAppTotal = { packageName: string; durationMillis: number };
export interface UsageTotalsStore { dailyAppTotals(deviceId: string, dayStart: string, dayEnd: string): Promise<DailyAppTotal[]>; }

export function postgresConnectionString(connectionString: string): string {
    const url = new URL(connectionString);
    if (url.searchParams.get("sslmode") === "require" && !url.searchParams.has("uselibpqcompat")) {
        url.searchParams.set("uselibpqcompat", "true");
    }
    return url.toString();
}

export class PostgresEventStore implements EventStore, UsageTotalsStore {
    private readonly pool: Pool;
    constructor(connectionString: string) { this.pool = new Pool({ connectionString: postgresConnectionString(connectionString) }); }
    async insert(event: EventEnvelopeV1): Promise<InsertResult> {
        const result = await this.pool.query(
            `INSERT INTO events (id, device_id, schema_version, event_type, occurred_at, recorded_at, observed_timezone, source, sensitivity, payload, confidence, is_derived)
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12) ON CONFLICT (id) DO NOTHING`,
            [event.id, event.deviceId, event.schemaVersion, event.type, event.occurredAt, event.recordedAt, event.timezone, event.source, event.sensitivity, event.payload, event.quality.confidence, event.quality.isDerived],
        );
        return result.rowCount === 1 ? "accepted" : "duplicate";
    }
    async close() { await this.pool.end(); }
    async dailyAppTotals(deviceId: string, dayStart: string, dayEnd: string): Promise<DailyAppTotal[]> {
        const result = await this.pool.query(
            `SELECT payload->>'packageName' AS "packageName", SUM((payload->>'durationMillis')::bigint)::bigint AS "durationMillis"
             FROM events WHERE device_id=$1 AND event_type='usage.bucket' AND occurred_at >= $2 AND occurred_at < $3
             GROUP BY payload->>'packageName' ORDER BY "durationMillis" DESC, "packageName" ASC`, [deviceId, dayStart, dayEnd],
        );
        return result.rows.map((row) => ({ packageName: row.packageName, durationMillis: Number(row.durationMillis) }));
    }
}
