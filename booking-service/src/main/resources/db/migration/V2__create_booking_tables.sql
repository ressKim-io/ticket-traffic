-- Booking entity owned by booking-service
CREATE TABLE bookings (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    game_id         BIGINT          NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    total_price     DECIMAL(10,0)   NOT NULL DEFAULT 0,
    hold_expires_at TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_game_status ON bookings(game_id, status);
CREATE INDEX idx_bookings_hold_expiry ON bookings(status, hold_expires_at);

-- Booking-seat join table
CREATE TABLE booking_seats (
    id              BIGSERIAL       PRIMARY KEY,
    booking_id      BIGINT          NOT NULL REFERENCES bookings(id),
    game_seat_id    BIGINT          NOT NULL,
    price           DECIMAL(10,0)   NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_seats_booking ON booking_seats(booking_id);
CREATE UNIQUE INDEX uk_booking_seats_game_seat ON booking_seats(game_seat_id, booking_id);

-- Add version column for optimistic locking on local_game_seats
ALTER TABLE local_game_seats ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
