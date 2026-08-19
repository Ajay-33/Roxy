import { buildApp } from "./app.js";
import { loadMigrations, migrateDatabase, PostgresEventStore } from "@roxy/database";
import { fileURLToPath } from "node:url";

const key = process.env.ROXY_DEVICE_API_KEY;
if (key === undefined) throw new Error("ROXY_DEVICE_API_KEY is required");
const databaseUrl = process.env.DATABASE_URL;
if (databaseUrl === undefined) throw new Error("DATABASE_URL is required");
const migrations = await loadMigrations(fileURLToPath(new URL("../../../packages/database/migrations", import.meta.url)));
await migrateDatabase(databaseUrl, migrations);
const app = buildApp(key, new PostgresEventStore(databaseUrl));
await app.listen({ host: process.env.API_HOST ?? "0.0.0.0", port: Number(process.env.PORT ?? process.env.API_PORT ?? "4100") });
