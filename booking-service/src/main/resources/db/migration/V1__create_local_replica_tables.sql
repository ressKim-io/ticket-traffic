-- Local replica of game info (synced via Kafka: ticket.game.info-updated)
CREATE TABLE local_games (
    id                    BIGINT       PRIMARY KEY,
    home_team             VARCHAR(50)  NOT NULL,
    away_team             VARCHAR(50)  NOT NULL,
    game_date             TIMESTAMP    NOT NULL,
    ticket_open_at        TIMESTAMP    NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    max_tickets_per_user  INTEGER      NOT NULL DEFAULT 4,
    synced_at             TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_local_games_status ON local_games(status);

-- Local replica of game seats (synced via Kafka: ticket.game.seat-initialized)
CREATE TABLE local_game_seats (
    id          BIGINT         PRIMARY KEY,
    game_id     BIGINT         NOT NULL REFERENCES local_games(id),
    seat_id     BIGINT         NOT NULL,
    section_id  BIGINT         NOT NULL,
    price       DECIMAL(10,0)  NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    row_name    VARCHAR(10),
    seat_number INTEGER,
    synced_at   TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_local_game_seat ON local_game_seats(game_id, seat_id);
CREATE INDEX idx_local_game_seat_status ON local_game_seats(game_id, status);
CREATE INDEX idx_local_game_seat_section ON local_game_seats(game_id, section_id);
