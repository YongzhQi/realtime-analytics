CREATE TABLE IF NOT EXISTS web_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64),
    session_id VARCHAR(128),
    event_type VARCHAR(64),
    ts TIMESTAMP
    WITH
        TIME ZONE,
        payload JSONB
);

CREATE INDEX IF NOT EXISTS idx_web_events_ts ON web_events (ts);