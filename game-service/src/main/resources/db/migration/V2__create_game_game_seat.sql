CREATE TABLE games (
    id                    BIGSERIAL    PRIMARY KEY,
    stadium_id            BIGINT       NOT NULL REFERENCES stadiums(id),
    home_team             VARCHAR(50)  NOT NULL,
    away_team             VARCHAR(50)  NOT NULL,
    game_date             TIMESTAMP    NOT NULL,
    ticket_open_at        TIMESTAMP    NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    max_tickets_per_user  INTEGER      NOT NULL DEFAULT 4,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_games_stadium ON games(stadium_id);
CREATE INDEX idx_games_date ON games(game_date);
CREATE INDEX idx_games_status ON games(status);
CREATE INDEX idx_games_ticket_open ON games(ticket_open_at);

CREATE TABLE game_seats (
    id                  BIGSERIAL      PRIMARY KEY,
    game_id             BIGINT         NOT NULL REFERENCES games(id),
    seat_id             BIGINT         NOT NULL REFERENCES seats(id),
    price               DECIMAL(10,0)  NOT NULL,
    status              VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    held_by_booking_id  BIGINT,
    held_at             TIMESTAMP,
    version             BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_game_seat ON game_seats(game_id, seat_id);
CREATE INDEX idx_game_seat_status ON game_seats(game_id, status);
CREATE INDEX idx_game_seat_held ON game_seats(game_id, held_by_booking_id) WHERE held_by_booking_id IS NOT NULL;
