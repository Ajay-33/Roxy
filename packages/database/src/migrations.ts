import { readFile, readdir } from "node:fs/promises";
import { Pool } from "pg";
import { postgresConnectionString } from "./event-store.ts";

export type SqlMigration = { id: string; sql: string };
export type MigrationQueryResult<Row = { id: string }> = { rows: Row[] };
export interface MigrationClient {
    query<Row = { id: string }>(sql: string, values?: readonly unknown[]): Promise<MigrationQueryResult<Row>>;
}

export async function loadMigrations(directory: string): Promise<SqlMigration[]> {
    const names = (await readdir(directory)).filter((name) => name.endsWith(".up.sql")).sort();
    return Promise.all(names.map(async (name) => ({
        id: name.slice(0, -".up.sql".length),
        sql: await readFile(`${directory}/${name}`, "utf8"),
    })));
}

export async function applyMigrations(client: MigrationClient, migrations: readonly SqlMigration[]): Promise<void> {
    await client.query("CREATE TABLE IF NOT EXISTS schema_migrations (id text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())");
    const applied = new Set((await client.query<{ id: string }>("SELECT id FROM schema_migrations")).rows.map((row) => row.id));
    for (const migration of migrations) {
        if (applied.has(migration.id)) continue;
        await client.query("BEGIN");
        try {
            await client.query(migration.sql);
            await client.query("INSERT INTO schema_migrations (id) VALUES ($1)", [migration.id]);
            await client.query("COMMIT");
        } catch (error) {
            await client.query("ROLLBACK");
            throw error;
        }
    }
}

export async function migrateDatabase(connectionString: string, migrations: readonly SqlMigration[]): Promise<void> {
    const pool = new Pool({ connectionString: postgresConnectionString(connectionString) });
    try {
        await applyMigrations(pool, migrations);
    } finally {
        await pool.end();
    }
}
