-- T-0003 rollback. Keep the pgvector extension available for later migrations.
DROP TABLE IF EXISTS roxy_migration_smoke;
