# Infrastructure

`docker-compose.yml` starts a development-only PostgreSQL instance with pgvector. Its password is intentionally local and must never be reused for a deployed environment.

Backup and restore tooling will be implemented and tested before the MVP exit gate. No deployment configuration is chosen yet.

