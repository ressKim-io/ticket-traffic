package com.sportstix.payment.jooq;

import org.jooq.DSLContext;
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

import static com.sportstix.payment.jooq.generated.Tables.LOCAL_BOOKINGS;
import static org.assertj.core.api.Assertions.assertThat;

class LocalBookingJooqRepositoryTest {

    private static Connection connection;
    private DSLContext dsl;
    private LocalBookingJooqRepository repository;

    @BeforeAll
    static void initDb() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:payment_test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
        DSLContext setup = DSL.using(connection, SQLDialect.H2);

        setup.execute("""
                CREATE TABLE IF NOT EXISTS local_bookings (
                    id BIGINT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    game_id BIGINT NOT NULL,
                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                    total_price DECIMAL(10,0),
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
        dsl.deleteFrom(LOCAL_BOOKINGS).execute();

        dsl.insertInto(LOCAL_BOOKINGS)
                .set(LOCAL_BOOKINGS.ID, 1L)
                .set(LOCAL_BOOKINGS.USER_ID, 100L)
                .set(LOCAL_BOOKINGS.GAME_ID, 10L)
                .set(LOCAL_BOOKINGS.STATUS, "PENDING")
                .set(LOCAL_BOOKINGS.SYNCED_AT, LocalDateTime.now())
                .execute();

        dsl.insertInto(LOCAL_BOOKINGS)
                .set(LOCAL_BOOKINGS.ID, 2L)
                .set(LOCAL_BOOKINGS.USER_ID, 100L)
                .set(LOCAL_BOOKINGS.GAME_ID, 20L)
                .set(LOCAL_BOOKINGS.STATUS, "CONFIRMED")
                .set(LOCAL_BOOKINGS.SYNCED_AT, LocalDateTime.now())
                .execute();

        repository = new LocalBookingJooqRepository(dsl);
    }

    @Test
    void findByIdForUpdate_returnsMatchingBooking() {
        var result = repository.findByIdForUpdate(1L, "PENDING");
        assertThat(result).isNotNull();
        assertThat(result.get(LOCAL_BOOKINGS.ID)).isEqualTo(1L);
    }

    @Test
    void findByIdForUpdate_wrongStatus_returnsNull() {
        var result = repository.findByIdForUpdate(1L, "CONFIRMED");
        assertThat(result).isNull();
    }

    @Test
    void updateStatus_updatesSuccessfully() {
        int updated = repository.updateStatus(1L, "CONFIRMED");
        assertThat(updated).isEqualTo(1);

        String status = dsl.select(LOCAL_BOOKINGS.STATUS)
                .from(LOCAL_BOOKINGS)
                .where(LOCAL_BOOKINGS.ID.eq(1L))
                .fetchOne(LOCAL_BOOKINGS.STATUS);
        assertThat(status).isEqualTo("CONFIRMED");
    }

    @Test
    void updateStatus_notFound_returnsZero() {
        assertThat(repository.updateStatus(999L, "CONFIRMED")).isEqualTo(0);
    }

    @Test
    void countByUserAndGameAndStatus_countsCorrectly() {
        assertThat(repository.countByUserAndGameAndStatus(100L, 10L, "PENDING")).isEqualTo(1);
    }

    @Test
    void countByUserAndGameAndStatus_noMatch_returnsZero() {
        assertThat(repository.countByUserAndGameAndStatus(100L, 10L, "CANCELLED")).isEqualTo(0);
    }
}
