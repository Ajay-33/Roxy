import Fastify from "fastify";
import { eventEnvelopeV1Schema } from "@roxy/contracts";
import { timingSafeEqual } from "node:crypto";
import type { EventStore } from "@roxy/database";

const maxBatchEvents = 250;
const maxBodyBytes = 256 * 1024;

function credentialsMatch(received: string | undefined, expected: string): boolean {
    if (!received?.startsWith("Bearer ")) return false;
    const provided = Buffer.from(received.slice("Bearer ".length));
    const configured = Buffer.from(expected);
    return provided.length === configured.length && timingSafeEqual(provided, configured);
}

export function buildApp(deviceApiKey: string, eventStore: EventStore) {
    if (deviceApiKey.length < 32) throw new Error("ROXY_DEVICE_API_KEY must be at least 32 characters");
    const app = Fastify({ bodyLimit: maxBodyBytes, logger: false });

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
    return app;
}
