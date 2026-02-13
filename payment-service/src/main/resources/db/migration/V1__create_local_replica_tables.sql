-- Local replica of booking info (synced via Kafka: ticket.booking.created/confirmed/cancelled)
CREATE TABLE local_bookings (
    id          BIGINT         PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    game_id     BIGINT         NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10,0),
    synced_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_local_bookings_user ON local_bookings(user_id);
CREATE INDEX idx_local_bookings_status ON local_bookings(status);
