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
export type NotificationSummary = { count: number; items: Array<{ id: string; type: string; occurredAt: string; packageName: string; redactionCount: number }> };
export interface NotificationSummaryStore { notificationSummary(query: { deviceId: string; date: string; limit: number }): Promise<NotificationSummary>; }
export type NotificationAnalytics = { count: number; postedCount: number; updatedCount: number; removedCount: number; hourly: Array<{ hour: number; count: number }>; hourlySources: Array<{ hour: number; sources: Array<{ packageName: string; count: number }> }>; history: Array<{ date: string; count: number }>; topApps: Array<{ packageName: string; count: number }>; bursts: Array<{ startHour: number; count: number }>; period: "day" | "week" | "month" };
export interface NotificationAnalyticsStore { notificationAnalytics(query: { deviceId: string; date: string; period: "day" | "week" | "month"; limit: number }): Promise<NotificationAnalytics>; }
export type TimelineStore = { timeline(query: { deviceId: string; date: string; type?: "usage.bucket"; limit: number; cursor?: { occurredAt: string; id: string } }): Promise<TimelinePage> };

export function postgresConnectionString(connectionString: string): string {
    const url = new URL(connectionString);
    if (url.searchParams.get("sslmode") === "require" && !url.searchParams.has("uselibpqcompat")) {
        url.searchParams.set("uselibpqcompat", "true");
    }
    return url.toString();
}

export class PostgresEventStore implements EventStore, UsageTotalsStore, TimelineStore, UsageSummaryStore, NotificationSummaryStore, NotificationAnalyticsStore {
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
    async notificationSummary(query: { deviceId: string; date: string; limit: number }): Promise<NotificationSummary> {
        const filters = [query.deviceId, query.date];
        const count = await this.pool.query(`SELECT COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date`, filters);
        const rows = await this.pool.query(`SELECT id, event_type AS type, occurred_at AS "occurredAt", payload->>'packageName' AS "packageName", COALESCE((payload->>'redactionCount')::integer,0) AS "redactionCount" FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND (occurred_at AT TIME ZONE observed_timezone)::date=$2::date ORDER BY occurred_at DESC,id DESC LIMIT $3`, [...filters, query.limit]);
        return { count: Number(count.rows[0].count), items: rows.rows.map((row) => ({ ...row, occurredAt: new Date(row.occurredAt).toISOString(), redactionCount: Number(row.redactionCount) })) };
    }
    async notificationAnalytics(query: { deviceId: string; date: string; period: "day" | "week" | "month"; limit: number }): Promise<NotificationAnalytics> {
        const filters = [query.deviceId, query.date, query.period];
        const periodFilter = `(occurred_at AT TIME ZONE observed_timezone)::date >= CASE $3 WHEN 'day' THEN $2::date WHEN 'week' THEN $2::date - 6 ELSE date_trunc('month', $2::date)::date END AND (occurred_at AT TIME ZONE observed_timezone)::date <= $2::date`;
        const totals = await this.pool.query(`SELECT COUNT(*)::integer AS count, COUNT(*) FILTER (WHERE event_type='notification.posted')::integer AS "postedCount", COUNT(*) FILTER (WHERE event_type='notification.updated')::integer AS "updatedCount", COUNT(*) FILTER (WHERE event_type='notification.removed')::integer AS "removedCount" FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND ${periodFilter}`, filters);
        const hourly = await this.pool.query(`SELECT EXTRACT(HOUR FROM occurred_at AT TIME ZONE observed_timezone)::integer AS hour, COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND ${periodFilter} GROUP BY hour ORDER BY hour`, filters);
        const hourlySources = await this.pool.query(`SELECT EXTRACT(HOUR FROM occurred_at AT TIME ZONE observed_timezone)::integer AS hour, payload->>'packageName' AS "packageName", COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND ${periodFilter} GROUP BY hour, "packageName" ORDER BY hour, count DESC, "packageName" ASC`, filters);
        const history = await this.pool.query(`SELECT (occurred_at AT TIME ZONE observed_timezone)::date::text AS date, COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND (occurred_at AT TIME ZONE observed_timezone)::date >= $2::date - 7 AND (occurred_at AT TIME ZONE observed_timezone)::date < $2::date GROUP BY date ORDER BY date`, filters.slice(0, 2));
        const topApps = await this.pool.query(`SELECT payload->>'packageName' AS "packageName", COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND ${periodFilter} GROUP BY "packageName" ORDER BY count DESC, "packageName" ASC LIMIT $4`, [...filters, query.limit]);
        const bursts = await this.pool.query(`SELECT EXTRACT(HOUR FROM occurred_at AT TIME ZONE observed_timezone)::integer AS "startHour", COUNT(*)::integer AS count FROM events WHERE device_id=$1 AND event_type IN ('notification.posted','notification.updated','notification.removed') AND ${periodFilter} GROUP BY "startHour" HAVING COUNT(*) >= 3 ORDER BY count DESC, "startHour" ASC LIMIT $4`, [...filters, query.limit]);
        const grouped = new Map<number, Array<{ packageName: string; count: number }>>(); hourlySources.rows.forEach((row) => { const hour = Number(row.hour); grouped.set(hour, [...(grouped.get(hour) ?? []), { packageName: row.packageName, count: Number(row.count) }]); });
        return { ...totals.rows[0], count: Number(totals.rows[0].count), postedCount: Number(totals.rows[0].postedCount), updatedCount: Number(totals.rows[0].updatedCount), removedCount: Number(totals.rows[0].removedCount), hourly: hourly.rows.map((row) => ({ hour: Number(row.hour), count: Number(row.count) })), hourlySources: [...grouped.entries()].map(([hour, sources]) => ({ hour, sources })), history: history.rows.map((row) => ({ date: row.date, count: Number(row.count) })), topApps: topApps.rows.map((row) => ({ packageName: row.packageName, count: Number(row.count) })), bursts: bursts.rows.map((row) => ({ startHour: Number(row.startHour), count: Number(row.count) })), period: query.period };
    }
}
