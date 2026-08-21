import { Pool } from "pg";
import type { EventEnvelopeV1 } from "@roxy/contracts";

export type InsertResult = "accepted" | "duplicate";
export interface EventStore { insert(event: EventEnvelopeV1): Promise<InsertResult>; close?(): Promise<void>; }
export type DailyAppTotal = { packageName: string; durationMillis: number };
export interface UsageTotalsStore { dailyAppTotals(deviceId: string, dayStart: string, dayEnd: string): Promise<DailyAppTotal[]>; }
export type UsageSummary = { totalDurationMillis: number; evidenceEventIds: string[]; topApps: Array<{ appId: string; durationMillis: number; evidenceEventIds: string[] }> };
export type UsageSummaryStore = { usageSummary(query: { deviceId: string; date: string; limit: number }): Promise<UsageSummary> };
export type TimelineItem = { id: string; type: string; occurredAt: string; recordedAt: string; timezone: string; source: string; confidence: number; isDerived: boolean };
export type TimelinePage = { items: TimelineItem[]; total: number };
export type TimelineStore = { timeline(query: { deviceId: string; date: string; type?: "usage.bucket"; limit: number; cursor?: { occurredAt: string; id: string } }): Promise<TimelinePage> };

export function postgresConnectionString(connectionString: string): string {
    const url = new URL(connectionString);
    if (url.searchParams.get("sslmode") === "require" && !url.searchParams.has("uselibpqcompat")) {
        url.searchParams.set("uselibpqcompat", "true");
    }
    return url.toString();
}

export class PostgresEventStore implements EventStore, UsageTotalsStore, TimelineStore, UsageSummaryStore {
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
    async usageSummary(query: { deviceId: string; date: string; limit: number }): Promise<UsageSummary> {
        const filters = [query.deviceId, query.date];
        const total = await this.pool.query(
            `SELECT COALESCE(SUM((payload->>'durationMillis')::bigint), 0)::bigint AS "totalDurationMillis",
                    COALESCE(ARRAY_AGG(id ORDER BY occurred_at ASC, id ASC), ARRAY[]::text[]) AS "evidenceEventIds"
             FROM events
             WHERE device_id=$1 AND event_type='usage.bucket'
               AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date`,
            filters,
        );
        const topApps = await this.pool.query(
            `SELECT payload->>'packageName' AS "appId", SUM((payload->>'durationMillis')::bigint)::bigint AS "durationMillis",
                    ARRAY_AGG(id ORDER BY occurred_at ASC, id ASC) AS "evidenceEventIds"
             FROM events
             WHERE device_id=$1 AND event_type='usage.bucket'
               AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date
             GROUP BY payload->>'packageName'
             ORDER BY "durationMillis" DESC, "appId" ASC
             LIMIT $3`,
            [...filters, query.limit],
        );
        return {
            totalDurationMillis: Number(total.rows[0].totalDurationMillis),
            evidenceEventIds: total.rows[0].evidenceEventIds ?? [],
            topApps: topApps.rows.map((row) => ({ appId: row.appId, durationMillis: Number(row.durationMillis), evidenceEventIds: row.evidenceEventIds ?? [] })),
        };
    }
    async timeline(query: { deviceId: string; date: string; type?: "usage.bucket"; limit: number; cursor?: { occurredAt: string; id: string } }): Promise<TimelinePage> {
        const filters = [query.deviceId, query.date, query.type ?? null];
        const total = await this.pool.query(
            `SELECT COUNT(*)::integer AS total
             FROM events
             WHERE device_id=$1
               AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date
               AND ($3::text IS NULL OR event_type=$3)`,
            filters,
        );
        const rows = await this.pool.query(
            `SELECT id, event_type AS type, occurred_at AS "occurredAt", recorded_at AS "recordedAt", observed_timezone AS timezone, source, confidence, is_derived AS "isDerived"
             FROM events
             WHERE device_id=$1
               AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date
               AND ($3::text IS NULL OR event_type=$3)
               AND ($4::timestamptz IS NULL OR (occurred_at, id) < ($4::timestamptz, $5::text))
             ORDER BY occurred_at DESC, id DESC
             LIMIT $6`,
            [...filters, query.cursor?.occurredAt ?? null, query.cursor?.id ?? null, query.limit],
        );
        return {
            total: Number(total.rows[0].total),
            items: rows.rows.map((row) => ({ ...row, occurredAt: new Date(row.occurredAt).toISOString(), recordedAt: new Date(row.recordedAt).toISOString(), confidence: Number(row.confidence) })),
        };
    }
}
