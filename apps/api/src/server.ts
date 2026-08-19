import { buildApp } from "./app.js";
import { PostgresEventStore } from "@roxy/database";

const key = process.env.ROXY_DEVICE_API_KEY;
if (key === undefined) throw new Error("ROXY_DEVICE_API_KEY is required");
const databaseUrl = process.env.DATABASE_URL;
if (databaseUrl === undefined) throw new Error("DATABASE_URL is required");
const app = buildApp(key, new PostgresEventStore(databaseUrl));
await app.listen({ host: process.env.API_HOST ?? "127.0.0.1", port: Number(process.env.API_PORT ?? "4100") });
