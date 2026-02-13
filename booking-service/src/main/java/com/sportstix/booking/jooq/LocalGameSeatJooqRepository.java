package com.sportstix.booking.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAME_SEATS;

/**
 * jOOQ repository for hot path seat operations.
 * Handles: pessimistic locking, bulk status updates, available seat queries.
 */
@Repository
@RequiredArgsConstructor
public class LocalGameSeatJooqRepository {

    private final DSLContext dsl;

    /**
     * Select a single seat with FOR UPDATE lock (pessimistic lock for booking).
     * Returns null if seat not found or not in expected status.
     */
    public Record findByIdForUpdate(Long gameSeatId, String expectedStatus) {
        return dsl.select()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(gameSeatId))
                .and(LOCAL_GAME_SEATS.STATUS.eq(expectedStatus))
                .forUpdate()
                .fetchOne();
    }

    /**
     * Bulk update seat status by IDs.
     * Used for holding/reserving/releasing multiple seats at once.
     */
    public int bulkUpdateStatus(Collection<Long> seatIds, String newStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }
        return dsl.update(LOCAL_GAME_SEATS)
                .set(LOCAL_GAME_SEATS.STATUS, newStatus)
                .set(LOCAL_GAME_SEATS.SYNCED_AT, LocalDateTime.now())
                .where(LOCAL_GAME_SEATS.ID.in(seatIds))
                .execute();
    }

    /**
     * Find available seats by game and section.
     * Hot path query for seat selection page.
     */
    public Result<Record> findAvailableByGameAndSection(Long gameId, Long sectionId) {
        return dsl.select()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.GAME_ID.eq(gameId))
                .and(LOCAL_GAME_SEATS.SECTION_ID.eq(sectionId))
                .and(LOCAL_GAME_SEATS.STATUS.eq("AVAILABLE"))
                .orderBy(LOCAL_GAME_SEATS.ROW_NAME, LOCAL_GAME_SEATS.SEAT_NUMBER)
                .fetch();
    }

    /**
     * Count available seats by game.
     */
    public long countAvailableByGame(Long gameId) {
        return dsl.selectCount()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.GAME_ID.eq(gameId))
                .and(LOCAL_GAME_SEATS.STATUS.eq("AVAILABLE"))
                .fetchOne(0, long.class);
    }

    /**
     * Count available seats by game and section.
     */
    public long countAvailableByGameAndSection(Long gameId, Long sectionId) {
        return dsl.selectCount()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.GAME_ID.eq(gameId))
                .and(LOCAL_GAME_SEATS.SECTION_ID.eq(sectionId))
                .and(LOCAL_GAME_SEATS.STATUS.eq("AVAILABLE"))
                .fetchOne(0, long.class);
    }

    /**
     * Select multiple seats with FOR UPDATE SKIP LOCKED.
     * Used when multiple users are trying to book seats concurrently.
     * SKIP LOCKED prevents blocking - returns only unlocked rows.
     */
    public Result<Record> findByIdsForUpdateSkipLocked(Collection<Long> seatIds, String expectedStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return dsl.newResult();
        }
        return dsl.select()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.in(seatIds))
                .and(LOCAL_GAME_SEATS.STATUS.eq(expectedStatus))
                .forUpdate()
                .skipLocked()
                .fetch();
    }

    /**
     * Get seat price by ID.
     */
    public BigDecimal getSeatPrice(Long gameSeatId) {
        Long price = dsl.select(LOCAL_GAME_SEATS.PRICE)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(gameSeatId))
                .fetchOne(LOCAL_GAME_SEATS.PRICE);
        return price != null ? BigDecimal.valueOf(price) : null;
    }
}
