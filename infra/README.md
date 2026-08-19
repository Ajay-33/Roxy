# Infrastructure

`docker-compose.yml` starts a development-only PostgreSQL instance with pgvector. Its password is intentionally local and must never be reused for a deployed environment.

## Local verification

From `infra/`, run `docker compose up -d` and wait for `docker compose ps` to report `healthy`.
In PowerShell, apply the Phase 0 smoke migration with:

```text
Get-Content -Raw migrations/0000_pgvector_smoke.up.sql | docker compose exec -T postgres psql -U roxy -d roxy -v ON_ERROR_STOP=1
```

Use the matching `.down.sql` file with the same command to verify rollback. Both files are disposable smoke checks, not product schema.

Backup and restore tooling will be implemented and tested before the MVP exit gate. No deployment configuration is chosen yet.
