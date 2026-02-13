package com.sportstix.booking.jooq;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;

import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAMES;
import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAME_SEATS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for LocalGameSeatJooqRepository using in-memory H2 + jOOQ.
 */
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
        if (connection != null) {
            connection.close();
        }
    }

    @BeforeEach
    void setUp() {
        dsl = DSL.using(connection, SQLDialect.H2);

        dsl.deleteFrom(LOCAL_GAME_SEATS).execute();
        dsl.deleteFrom(LOCAL_GAMES).execute();

        // Insert test game
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

        // Insert 5 test seats: seats 1-3 in section 1, seats 4-5 in section 2
        for (int i = 1; i <= 5; i++) {
            dsl.insertInto(LOCAL_GAME_SEATS)
                    .set(LOCAL_GAME_SEATS.ID, (long) i)
                    .set(LOCAL_GAME_SEATS.GAME_ID, 1L)
                    .set(LOCAL_GAME_SEATS.SEAT_ID, (long) (100 + i))
                    .set(LOCAL_GAME_SEATS.SECTION_ID, i <= 3 ? 1L : 2L)
                    .set(LOCAL_GAME_SEATS.PRICE, 50000L)
                    .set(LOCAL_GAME_SEATS.STATUS, "AVAILABLE")
                    .set(LOCAL_GAME_SEATS.ROW_NAME, "A")
                    .set(LOCAL_GAME_SEATS.SEAT_NUMBER, i)
                    .set(LOCAL_GAME_SEATS.SYNCED_AT, LocalDateTime.now())
                    .execute();
        }

        repository = new LocalGameSeatJooqRepository(dsl);
    }

    @Test
    void findByIdForUpdate_returnsMatchingSeat() {
        Record result = repository.findByIdForUpdate(1L, "AVAILABLE");
        assertThat(result).isNotNull();
        assertThat(result.get(LOCAL_GAME_SEATS.ID)).isEqualTo(1L);
    }

    @Test
    void findByIdForUpdate_wrongStatus_returnsNull() {
        Record result = repository.findByIdForUpdate(1L, "HELD");
        assertThat(result).isNull();
    }

    @Test
    void bulkUpdateStatus_updatesMultipleSeats() {
        int updated = repository.bulkUpdateStatus(Set.of(1L, 2L, 3L), "HELD");
        assertThat(updated).isEqualTo(3);

        String status = dsl.select(LOCAL_GAME_SEATS.STATUS)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(1L))
                .fetchOne(LOCAL_GAME_SEATS.STATUS);
        assertThat(status).isEqualTo("HELD");
    }

    @Test
    void bulkUpdateStatus_emptyCollection_returnsZero() {
        int updated = repository.bulkUpdateStatus(Set.of(), "HELD");
        assertThat(updated).isEqualTo(0);
    }

    @Test
    void findAvailableByGameAndSection_returnsFilteredSeats() {
        Result<Record> result = repository.findAvailableByGameAndSection(1L, 1L);
        assertThat(result).hasSize(3);
    }

    @Test
    void countAvailableByGame_countsCorrectly() {
        long count = repository.countAvailableByGame(1L);
        assertThat(count).isEqualTo(5);

        repository.bulkUpdateStatus(Set.of(1L, 2L), "HELD");
        count = repository.countAvailableByGame(1L);
        assertThat(count).isEqualTo(3);
    }

    @Test
    void countAvailableByGameAndSection_countsPerSection() {
        long section1Count = repository.countAvailableByGameAndSection(1L, 1L);
        long section2Count = repository.countAvailableByGameAndSection(1L, 2L);
        assertThat(section1Count).isEqualTo(3);
        assertThat(section2Count).isEqualTo(2);
    }

    @Test
    void getSeatPrice_returnsPrice() {
        BigDecimal price = repository.getSeatPrice(1L);
        assertThat(price).isEqualByComparingTo(BigDecimal.valueOf(50000));
    }

    @Test
    void getSeatPrice_notFound_returnsNull() {
        BigDecimal price = repository.getSeatPrice(999L);
        assertThat(price).isNull();
    }

    @Test
    void findByIdsForUpdateSkipLocked_returnsMatchingSeats() {
        Result<Record> result = repository.findByIdsForUpdateSkipLocked(
                Set.of(1L, 2L, 3L), "AVAILABLE");
        assertThat(result).hasSize(3);
    }

    @Test
    void findByIdsForUpdateSkipLocked_emptyIds_returnsEmpty() {
        Result<Record> result = repository.findByIdsForUpdateSkipLocked(
                Set.of(), "AVAILABLE");
        assertThat(result).isEmpty();
    }
}
