import Fastify from "fastify";
import { eventEnvelopeV1Schema, timelineQuerySchema, usageSummaryQuerySchema } from "@roxy/contracts";
import { timingSafeEqual } from "node:crypto";
import type { EventStore, TimelineStore, UsageSummaryStore, UsageTotalsStore } from "@roxy/database";

const maxBatchEvents = 250;
const maxBodyBytes = 256 * 1024;

function credentialsMatch(received: string | undefined, expected: string): boolean {
    if (!received?.startsWith("Bearer ")) return false;
    const provided = Buffer.from(received.slice("Bearer ".length));
    const configured = Buffer.from(expected);
    return provided.length === configured.length && timingSafeEqual(provided, configured);
}

type TimelineCursor = { occurredAt: string; id: string };

function decodeTimelineCursor(value: string): TimelineCursor | undefined {
    try {
        const parsed: unknown = JSON.parse(Buffer.from(value, "base64url").toString("utf8"));
        if (typeof parsed !== "object" || parsed === null || Array.isArray(parsed)) return undefined;
        const cursor = parsed as Record<string, unknown>;
        if (typeof cursor.occurredAt !== "string" || Number.isNaN(Date.parse(cursor.occurredAt)) || typeof cursor.id !== "string" || cursor.id.length === 0) return undefined;
        return { occurredAt: cursor.occurredAt, id: cursor.id };
    } catch { return undefined; }
}

function encodeTimelineCursor(item: TimelineCursor): string {
    return Buffer.from(JSON.stringify(item)).toString("base64url");
}

export function buildApp(deviceApiKey: string, eventStore: EventStore & Partial<UsageTotalsStore & TimelineStore & UsageSummaryStore>) {
    if (deviceApiKey.length < 32) throw new Error("ROXY_DEVICE_API_KEY must be at least 32 characters");
    const app = Fastify({ bodyLimit: maxBodyBytes, logger: false });

    app.get("/health", async () => ({ status: "ok" }));

    app.post("/v1/sync/events", async (request, reply) => {
        if (!credentialsMatch(request.headers.authorization, deviceApiKey)) {
            return reply.code(401).send({ error: "unauthorized" });
        }
        const body = request.body as { events?: unknown[] };
        if (!Array.isArray(body?.events) || body.events.length > maxBatchEvents) {
            return reply.code(400).send({ error: "invalid_batch" });
        }
        const rejected = body.events.flatMap((event, index) => {
            const result = eventEnvelopeV1Schema.safeParse(event);
            return result.success ? [] : [{
                index,
                code: "invalid_event",
                fields: [...new Set(result.error.issues.map((issue) => issue.path.join(".") || "event"))],
            }];
        });
        const validEvents = body.events.flatMap((event, index) => {
            const result = eventEnvelopeV1Schema.safeParse(event);
            return result.success ? [{ index, event: result.data }] : [];
        });
        const acknowledgements = await Promise.all(validEvents.map(async ({ index, event }) => ({ index, id: event.id, status: await eventStore.insert(event) })));
        return reply.code(rejected.length === 0 ? 202 : 207).send({ acknowledgements, rejected });
    });
    app.get("/v1/usage/daily", async (request, reply) => {
        if (!credentialsMatch(request.headers.authorization, deviceApiKey)) return reply.code(401).send({ error: "unauthorized" });
        const query = request.query as { deviceId?: string; dayStart?: string; dayEnd?: string };
        if (!query.deviceId || !query.dayStart || !query.dayEnd || !eventStore.dailyAppTotals) return reply.code(400).send({ error: "invalid_usage_query" });
        return { totals: await eventStore.dailyAppTotals(query.deviceId, query.dayStart, query.dayEnd) };
    });
    app.get("/v1/usage/summary", async (request, reply) => {
        if (!credentialsMatch(request.headers.authorization, deviceApiKey)) return reply.code(401).send({ error: "unauthorized" });
        if (!eventStore.usageSummary) return reply.code(501).send({ error: "usage_summary_unavailable" });
        const parsed = usageSummaryQuerySchema.safeParse(request.query);
        if (!parsed.success) return reply.code(400).send({ error: "invalid_usage_summary_query" });
        const summary = await eventStore.usageSummary(parsed.data);
        return {
            ...summary,
            completeness: { status: "incomplete", reason: summary.totalDurationMillis === 0 ? "no_aggregate_data" : "coverage_not_proven" },
        };
    });
    app.get("/v1/timeline", async (request, reply) => {
        if (!credentialsMatch(request.headers.authorization, deviceApiKey)) return reply.code(401).send({ error: "unauthorized" });
        if (!eventStore.timeline) return reply.code(501).send({ error: "timeline_unavailable" });
        const parsed = timelineQuerySchema.safeParse(request.query);
        if (!parsed.success) return reply.code(400).send({ error: "invalid_timeline_query" });
        const cursor = parsed.data.cursor ? decodeTimelineCursor(parsed.data.cursor) : undefined;
        if (parsed.data.cursor && !cursor) return reply.code(400).send({ error: "invalid_timeline_cursor" });
        const timelineQuery = {
            deviceId: parsed.data.deviceId,
            date: parsed.data.date,
            limit: parsed.data.limit + 1,
            type: parsed.data.type,
            ...(cursor ? { cursor } : {}),
        };
        const page = await eventStore.timeline(timelineQuery);
        const items = page.items.slice(0, parsed.data.limit);
        const hasMore = page.items.length > parsed.data.limit;
        return {
            items,
            nextCursor: hasMore ? encodeTimelineCursor(items.at(-1)!) : null,
            completeness: { status: "incomplete", reason: page.total === 0 ? "no_aggregate_data" : "coverage_not_proven" },
        };
    });
    return app;
}
