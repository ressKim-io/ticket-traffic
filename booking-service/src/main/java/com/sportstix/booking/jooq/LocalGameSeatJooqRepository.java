package com.sportstix.booking.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static com.sportstix.booking.jooq.generated.Tables.LOCAL_GAME_SEATS;

/**
 * jOOQ repository for hot path seat operations.
 * Handles: pessimistic locking, bulk status updates, available seat queries.
 * All locking methods MUST be called within an active transaction.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalGameSeatJooqRepository {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String HELD = "HELD";
    public static final String RESERVED = "RESERVED";

    private final DSLContext dsl;

    /**
     * Select a single seat with FOR UPDATE lock (pessimistic lock for booking).
     * Returns null if seat not found or not in expected status.
     */
    public Record4<Long, Long, Long, String> findByIdForUpdate(Long gameSeatId, String expectedStatus) {
        return dsl.select(
                        LOCAL_GAME_SEATS.ID,
                        LOCAL_GAME_SEATS.GAME_ID,
                        LOCAL_GAME_SEATS.PRICE,
                        LOCAL_GAME_SEATS.STATUS)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(gameSeatId))
                .and(LOCAL_GAME_SEATS.STATUS.eq(expectedStatus))
                .forUpdate()
                .fetchOne();
    }

    /**
     * Bulk update seat status by IDs with current-status guard.
     * Returns the number of rows actually updated.
     */
    @Transactional
    public int bulkUpdateStatus(Collection<Long> seatIds, String expectedCurrentStatus, String newStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0;
        }
        return dsl.update(LOCAL_GAME_SEATS)
                .set(LOCAL_GAME_SEATS.STATUS, newStatus)
                .set(LOCAL_GAME_SEATS.SYNCED_AT, DSL.currentLocalDateTime())
                .where(LOCAL_GAME_SEATS.ID.in(seatIds))
                .and(LOCAL_GAME_SEATS.STATUS.eq(expectedCurrentStatus))
                .execute();
    }

    /**
     * Find available seats by game and section with pagination.
     * Hot path query for seat selection page.
     */
    public Result<Record4<Long, String, Integer, Long>> findAvailableByGameAndSection(
            Long gameId, Long sectionId, int limit, int offset) {
        return dsl.select(
                        LOCAL_GAME_SEATS.ID,
                        LOCAL_GAME_SEATS.ROW_NAME,
                        LOCAL_GAME_SEATS.SEAT_NUMBER,
                        LOCAL_GAME_SEATS.PRICE)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.GAME_ID.eq(gameId))
                .and(LOCAL_GAME_SEATS.SECTION_ID.eq(sectionId))
                .and(LOCAL_GAME_SEATS.STATUS.eq(AVAILABLE))
                .orderBy(LOCAL_GAME_SEATS.ROW_NAME, LOCAL_GAME_SEATS.SEAT_NUMBER)
                .limit(limit)
                .offset(offset)
                .fetch();
    }

    /**
     * Count available seats by game.
     */
    public long countAvailableByGame(Long gameId) {
        return dsl.selectCount()
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.GAME_ID.eq(gameId))
                .and(LOCAL_GAME_SEATS.STATUS.eq(AVAILABLE))
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
                .and(LOCAL_GAME_SEATS.STATUS.eq(AVAILABLE))
                .fetchOne(0, long.class);
    }

    /**
     * Select multiple seats with FOR UPDATE SKIP LOCKED.
     * Ordered by ID to prevent deadlocks.
     * SKIP LOCKED prevents blocking - returns only unlocked rows.
     */
    public Result<Record4<Long, Long, Long, String>> findByIdsForUpdateSkipLocked(
            Collection<Long> seatIds, String expectedStatus) {
        if (seatIds == null || seatIds.isEmpty()) {
            return dsl.newResult(
                    LOCAL_GAME_SEATS.ID,
                    LOCAL_GAME_SEATS.GAME_ID,
                    LOCAL_GAME_SEATS.PRICE,
                    LOCAL_GAME_SEATS.STATUS);
        }
        return dsl.select(
                        LOCAL_GAME_SEATS.ID,
                        LOCAL_GAME_SEATS.GAME_ID,
                        LOCAL_GAME_SEATS.PRICE,
                        LOCAL_GAME_SEATS.STATUS)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.in(seatIds))
                .and(LOCAL_GAME_SEATS.STATUS.eq(expectedStatus))
                .orderBy(LOCAL_GAME_SEATS.ID.asc())
                .forUpdate()
                .skipLocked()
                .fetch();
    }

    /**
     * Get seat price by ID.
     */
    public Long getSeatPrice(Long gameSeatId) {
        return dsl.select(LOCAL_GAME_SEATS.PRICE)
                .from(LOCAL_GAME_SEATS)
                .where(LOCAL_GAME_SEATS.ID.eq(gameSeatId))
                .fetchOne(LOCAL_GAME_SEATS.PRICE);
    }
}
