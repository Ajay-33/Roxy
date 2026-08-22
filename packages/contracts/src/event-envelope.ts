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

const localDateSchema = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, "Expected a local date in YYYY-MM-DD format").refine(
  (date) => {
    const [yearText, monthText, dayText] = date.split("-");
    const year = Number(yearText ?? "");
    const month = Number(monthText ?? "");
    const day = Number(dayText ?? "");
    const parsed = new Date(Date.UTC(year, month - 1, day));
    return parsed.getUTCFullYear() === year && parsed.getUTCMonth() === month - 1 && parsed.getUTCDate() === day;
  },
  "Expected a valid calendar date",
);

export const timelineQuerySchema = z.object({
  deviceId: z.string().min(1).max(128),
  date: localDateSchema,
  type: z.literal("usage.bucket").default("usage.bucket"),
  limit: z.coerce.number().int().min(1).max(100).default(50),
  cursor: z.string().min(1).max(512).optional(),
}).strict();
export type TimelineQuery = z.infer<typeof timelineQuerySchema>;

export const usageSummaryQuerySchema = z.object({
  deviceId: z.string().min(1).max(128),
  date: localDateSchema,
  limit: z.coerce.number().int().min(1).max(20).default(5),
}).strict();
export type UsageSummaryQuery = z.infer<typeof usageSummaryQuerySchema>;
export const notificationSummaryQuerySchema = z.object({ deviceId: z.string().min(1).max(128), date: localDateSchema, limit: z.coerce.number().int().min(1).max(100).default(50) }).strict();
export type NotificationSummaryQuery = z.infer<typeof notificationSummaryQuerySchema>;

const notificationPayloadSchema = z.object({
  packageName: z.string().regex(/^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$/),
  identityDigest: z.string().regex(/^[0-9a-f]{64}$/i),
  redactionCount: z.number().int().min(0).max(100).default(0),
}).strict();

const eventEnvelopeBaseSchema = z.object({
  id: uuidV7Schema, schemaVersion: z.literal(1), deviceId: z.string().min(1),
  type: z.string().regex(/^[a-z][a-z0-9]*(\.[a-z][a-z0-9_]*)+$/), occurredAt: z.string().datetime(), recordedAt: z.string().datetime(),
  timezone: timezoneSchema, source: z.string().regex(/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/), sensitivity: z.enum(["private", "highly_sensitive"]),
  payload: z.record(z.unknown()), quality: z.object({ confidence: z.number().min(0).max(1), isDerived: z.boolean() }),
});
export const eventEnvelopeV1Schema = eventEnvelopeBaseSchema.superRefine((event, context) => {
  if (event.type === "notification.posted" || event.type === "notification.removed") {
    const parsed = notificationPayloadSchema.safeParse(event.payload);
    if (!parsed.success) context.addIssue({ code: z.ZodIssueCode.custom, message: "Expected metadata-only notification payload", path: ["payload"] });
  }
});
export type EventEnvelopeV1 = z.infer<typeof eventEnvelopeV1Schema>;
