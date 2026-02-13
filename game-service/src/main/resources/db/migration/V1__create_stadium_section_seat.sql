CREATE TABLE stadiums (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    address        VARCHAR(200) NOT NULL,
    total_capacity INT          NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE sections (
    id         BIGSERIAL   PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    grade      VARCHAR(20) NOT NULL,
    capacity   INT         NOT NULL,
    stadium_id BIGINT      NOT NULL REFERENCES stadiums(id),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sections_stadium ON sections(stadium_id);

CREATE TABLE seats (
    id         BIGSERIAL   PRIMARY KEY,
    row_number VARCHAR(10) NOT NULL,
    seat_number INT        NOT NULL,
    section_id BIGINT      NOT NULL REFERENCES sections(id),
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seats_section ON seats(section_id);
CREATE UNIQUE INDEX idx_seats_section_row_number ON seats(section_id, row_number, seat_number);
