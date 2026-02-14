-- Admin service tables for dashboard stats and event tracking

-- Aggregated booking stats per game
CREATE TABLE booking_stats (
    id          BIGSERIAL PRIMARY KEY,
    game_id     BIGINT      NOT NULL UNIQUE,
    home_team   VARCHAR(100),
    away_team   VARCHAR(100),
    total_bookings      INT NOT NULL DEFAULT 0,
    confirmed_bookings  INT NOT NULL DEFAULT 0,
    cancelled_bookings  INT NOT NULL DEFAULT 0,
    total_revenue       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    total_refunds       NUMERIC(15, 2) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_stats_game_id ON booking_stats(game_id);

-- Processed events for idempotency
CREATE TABLE processed_events (
    event_id    VARCHAR(64) PRIMARY KEY,
    topic       VARCHAR(128) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
