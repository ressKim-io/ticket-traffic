-- Idempotency tracking for Kafka consumer deduplication
CREATE TABLE processed_events (
    event_id     VARCHAR(36)    PRIMARY KEY,
    topic        VARCHAR(100)   NOT NULL,
    processed_at TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_processed_events_topic ON processed_events(topic, processed_at);
