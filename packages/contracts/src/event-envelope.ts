import { z } from "zod";

export const eventEnvelopeV1Schema = z.object({
  id: z.string().uuid(), schemaVersion: z.literal(1), deviceId: z.string().min(1),
  type: z.string().min(1), occurredAt: z.string().datetime(), recordedAt: z.string().datetime(),
  timezone: z.string().min(1), source: z.string().min(1), sensitivity: z.enum(["private", "highly_sensitive"]),
  payload: z.record(z.unknown()), quality: z.object({ confidence: z.number().min(0).max(1), isDerived: z.boolean() }),
});
export type EventEnvelopeV1 = z.infer<typeof eventEnvelopeV1Schema>;
