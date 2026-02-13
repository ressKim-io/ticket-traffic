package com.sportstix.booking.jooq;

import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;

import static com.sportstix.booking.jooq.LocalGameSeatJooqRepository.*;
import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAMES;
import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAME_SEATS;
import static org.assertj.core.api.Assertions.assertThat;

class LocalGameSeatJooqRepositoryTest {

    private static Connection connection;
    private DSLContext dsl;
    private LocalGameSeatJooqRepository repository;

    @BeforeAll
    static void initDb() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:booking_test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
        DSLContext setup = DSL.using(connection, SQLDialect.H2);

        setup.execute("""
                CREATE TABLE IF NOT EXISTS local_games (
                    id BIGINT PRIMARY KEY,
                    home_team VARCHAR(50) NOT NULL,
                    away_team VARCHAR(50) NOT NULL,
                    game_date TIMESTAMP NOT NULL,
                    ticket_open_at TIMESTAMP NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
                    max_tickets_per_user INTEGER NOT NULL DEFAULT 4,
                    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        setup.execute("""
                CREATE TABLE IF NOT EXISTS local_game_seats (
                    id BIGINT PRIMARY KEY,
                    game_id BIGINT NOT NULL REFERENCES local_games(id),
                    seat_id BIGINT NOT NULL,
                    section_id BIGINT NOT NULL,
                    price DECIMAL(10,0) NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                    row_name VARCHAR(10),
                    seat_number INTEGER,
                    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
    }

    @AfterAll
    static void closeDb() throws SQLException {
        if (connection != null) connection.close();
    }

    @BeforeEach
    void setUp() {
        dsl = DSL.using(connection, SQLDialect.H2);
        dsl.deleteFrom(LOCAL_GAME_SEATS).execute();
        dsl.deleteFrom(LOCAL_GAMES).execute();

        dsl.insertInto(LOCAL_GAMES)
                .set(LOCAL_GAMES.ID, 1L)
                .set(LOCAL_GAMES.HOME_TEAM, "Home")
                .set(LOCAL_GAMES.AWAY_TEAM, "Away")
                .set(LOCAL_GAMES.GAME_DATE, LocalDateTime.of(2025, 3, 15, 19, 0))
                .set(LOCAL_GAMES.TICKET_OPEN_AT, LocalDateTime.of(2025, 3, 10, 10, 0))
                .set(LOCAL_GAMES.STATUS, "SCHEDULED")
                .set(LOCAL_GAMES.MAX_TICKETS_PER_USER, 4)
                .set(LOCAL_GAMES.SYNCED_AT, LocalDateTime.now())
                .execute();

        for (int i = 1; i <= 5; i++) {
            dsl.insertInto(LOCAL_GAME_SEATS)
                    .set(LOCAL_GAME_SEATS.ID, (long) i)
                    .set(LOCAL_GAME_SEATS.GAME_ID, 1L)
                    .set(LOCAL_GAME_SEATS.SEAT_ID, (long) (100 + i))
                    .set(LOCAL_GAME_SEATS.SECTION_ID, i <= 3 ? 1L : 2L)
                    .set(LOCAL_GAME_SEATS.PRICE, 50000L)
                    .set(LOCAL_GAME_SEATS.STATUS, AVAILABLE)
                    .set(LOCAL_GAME_SEATS.ROW_NAME, "A")
                    .set(LOCAL_GAME_SEATS.SEAT_NUMBER, i)
                    .set(LOCAL_GAME_SEATS.SYNCED_AT, LocalDateTime.now())
                    .execute();
        }

        repository = new LocalGameSeatJooqRepository(dsl);
    }

    @Test
    void findByIdForUpdate_returnsMatchingSeat() {
        var result = repository.findByIdForUpdate(1L, AVAILABLE);
        assertThat(result).isNotNull();
        assertThat(result.get(LOCAL_GAME_SEATS.ID)).isEqualTo(1L);
    }

    @Test
    void findByIdForUpdate_wrongStatus_returnsNull() {
        var result = repository.findByIdForUpdate(1L, HELD);
        assertThat(result).isNull();
    }

    @Test
    void bulkUpdateStatus_updatesMatchingSeats() {
        int updated = repository.bulkUpdateStatus(Set.of(1L, 2L, 3L), AVAILABLE, HELD);
        assertThat(updated).isEqualTo(3);

        String status = dsl.select(LOCAL_GAME_SEATS.STATUS)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(1L))
                .fetchOne(LOCAL_GAME_SEATS.STATUS);
        assertThat(status).isEqualTo(HELD);
    }

    @Test
    void bulkUpdateStatus_wrongCurrentStatus_updatesNothing() {
        int updated = repository.bulkUpdateStatus(Set.of(1L, 2L), HELD, RESERVED);
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void bulkUpdateStatus_emptyCollection_returnsZero() {
        int updated = repository.bulkUpdateStatus(Set.of(), AVAILABLE, HELD);
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void findAvailableByGameAndSection_returnsFilteredSeats() {
        var result = repository.findAvailableByGameAndSection(1L, 1L, 100, 0);
        assertThat(result).hasSize(3);
    }

    @Test
    void findAvailableByGameAndSection_withPagination() {
        var page1 = repository.findAvailableByGameAndSection(1L, 1L, 2, 0);
        var page2 = repository.findAvailableByGameAndSection(1L, 1L, 2, 2);
        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(1);
    }

    @Test
    void countAvailableByGame_countsCorrectly() {
        assertThat(repository.countAvailableByGame(1L)).isEqualTo(5);

        repository.bulkUpdateStatus(Set.of(1L, 2L), AVAILABLE, HELD);
        assertThat(repository.countAvailableByGame(1L)).isEqualTo(3);
    }

    @Test
    void countAvailableByGameAndSection_countsPerSection() {
        assertThat(repository.countAvailableByGameAndSection(1L, 1L)).isEqualTo(3);
        assertThat(repository.countAvailableByGameAndSection(1L, 2L)).isEqualTo(2);
    }

    @Test
    void getSeatPrice_returnsPrice() {
        Long price = repository.getSeatPrice(1L);
        assertThat(price).isEqualTo(50000L);
    }

    @Test
    void getSeatPrice_notFound_returnsNull() {
        assertThat(repository.getSeatPrice(999L)).isNull();
    }

    @Test
    void findByIdsForUpdateSkipLocked_returnsMatchingSeats() {
        var result = repository.findByIdsForUpdateSkipLocked(Set.of(1L, 2L, 3L), AVAILABLE);
        assertThat(result).hasSize(3);
    }

    @Test
    void findByIdsForUpdateSkipLocked_emptyIds_returnsEmpty() {
        var result = repository.findByIdsForUpdateSkipLocked(Set.of(), AVAILABLE);
        assertThat(result).isEmpty();
    }

    @Test
    void bulkUpdateStatus_partialMatch_updatesOnlyMatching() {
        // Hold seats 1,2 first
        repository.bulkUpdateStatus(Set.of(1L, 2L), AVAILABLE, HELD);
        // Try to hold 2,3,4 - only 3,4 should match (2 is already HELD)
        int updated = repository.bulkUpdateStatus(Set.of(2L, 3L, 4L), AVAILABLE, HELD);
        assertThat(updated).isEqualTo(2);
    }
}
