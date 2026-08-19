# Database

SQL migrations and focused queries live here. The Phase 1 `events` migration stores device-supplied UUIDv7 IDs as the primary key, preserving idempotent ingestion, UTC instants, and observed IANA timezone separately.

Apply `migrations/0001_events.up.sql` only to the local PostgreSQL service; use the matching `.down.sql` during a controlled rollback. It contains no real data.
