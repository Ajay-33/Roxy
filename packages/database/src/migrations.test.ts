import assert from "node:assert/strict";
import test from "node:test";
import { applyMigrations, type MigrationClient } from "./migrations.ts";

function fakeClient(applied: string[] = []): { client: MigrationClient; queries: string[] } {
    const queries: string[] = [];
    return {
        queries,
        client: { query: async <Row = { id: string }>(sql: string) => {
            queries.push(sql);
            if (sql === "SELECT id FROM schema_migrations") return { rows: applied.map((id) => ({ id }) as Row) };
            return { rows: [] as Row[] };
        } },
    };
}

test("applies an unapplied migration and records its identifier", async () => {
    const { client, queries } = fakeClient();
    await applyMigrations(client, [{ id: "0001_events", sql: "CREATE TABLE events ()" }]);
    assert.deepEqual(queries.slice(-4), ["BEGIN", "CREATE TABLE events ()", "INSERT INTO schema_migrations (id) VALUES ($1)", "COMMIT"]);
});

test("skips a migration that is already recorded", async () => {
    const { client, queries } = fakeClient(["0001_events"]);
    await applyMigrations(client, [{ id: "0001_events", sql: "CREATE TABLE events ()" }]);
    assert.equal(queries.includes("CREATE TABLE events ()"), false);
});
