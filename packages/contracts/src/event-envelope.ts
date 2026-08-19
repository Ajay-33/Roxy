import { z } from "zod";

const uuidV7Schema = z.string().regex(
  /^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i,
  "Expected a UUIDv7",
);
const timezoneSchema = z.string().refine(
  (timezone) => {
    try { Intl.DateTimeFormat(undefined, { timeZone: timezone }); return true; } catch { return false; }
  },
  "Expected an IANA timezone",
);

export const eventEnvelopeV1Schema = z.object({
  id: uuidV7Schema, schemaVersion: z.literal(1), deviceId: z.string().min(1),
  type: z.string().regex(/^[a-z][a-z0-9]*(\.[a-z][a-z0-9_]*)+$/), occurredAt: z.string().datetime(), recordedAt: z.string().datetime(),
  timezone: timezoneSchema, source: z.string().regex(/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/), sensitivity: z.enum(["private", "highly_sensitive"]),
  payload: z.record(z.unknown()), quality: z.object({ confidence: z.number().min(0).max(1), isDerived: z.boolean() }),
});
export type EventEnvelopeV1 = z.infer<typeof eventEnvelopeV1Schema>;
