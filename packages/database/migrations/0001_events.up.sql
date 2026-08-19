CREATE TABLE events (
    id text PRIMARY KEY,
    device_id text NOT NULL,
    schema_version integer NOT NULL,
    event_type text NOT NULL,
    occurred_at timestamptz NOT NULL,
    recorded_at timestamptz NOT NULL,
    observed_timezone text NOT NULL,
    source text NOT NULL,
    sensitivity text NOT NULL,
    payload jsonb NOT NULL,
    confidence double precision NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
    is_derived boolean NOT NULL,
    ingested_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX events_device_occurred_at_idx ON events (device_id, occurred_at);
