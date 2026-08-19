import { Pool } from "pg";
import type { EventEnvelopeV1 } from "@roxy/contracts";

export type InsertResult = "accepted" | "duplicate";
export interface EventStore { insert(event: EventEnvelopeV1): Promise<InsertResult>; close?(): Promise<void>; }

export class PostgresEventStore implements EventStore {
    private readonly pool: Pool;
    constructor(connectionString: string) { this.pool = new Pool({ connectionString }); }
    async insert(event: EventEnvelopeV1): Promise<InsertResult> {
        const result = await this.pool.query(
            `INSERT INTO events (id, device_id, schema_version, event_type, occurred_at, recorded_at, observed_timezone, source, sensitivity, payload, confidence, is_derived)
             VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12) ON CONFLICT (id) DO NOTHING`,
            [event.id, event.deviceId, event.schemaVersion, event.type, event.occurredAt, event.recordedAt, event.timezone, event.source, event.sensitivity, event.payload, event.quality.confidence, event.quality.isDerived],
        );
        return result.rowCount === 1 ? "accepted" : "duplicate";
    }
    async close() { await this.pool.end(); }
}
