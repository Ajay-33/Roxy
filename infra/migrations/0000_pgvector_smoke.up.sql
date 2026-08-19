-- T-0003 smoke migration. This is intentionally disposable and contains no product data.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE roxy_migration_smoke (
    id integer PRIMARY KEY,
    embedding vector(3) NOT NULL
);
