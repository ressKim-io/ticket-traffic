package com.sportstix.payment.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

import static com.sportstix.payment.jooq.generated.Tables.LOCAL_BOOKINGS;

/**
 * jOOQ repository for hot path booking queries in payment-service.
 * Handles: payment eligibility checks, booking status queries.
 */
@Repository
@RequiredArgsConstructor
public class LocalBookingJooqRepository {

    private final DSLContext dsl;

    /**
     * Find booking with FOR UPDATE lock for payment processing.
     */
    public Record findByIdForUpdate(Long bookingId, String expectedStatus) {
        return dsl.select()
                .from(LOCAL_BOOKINGS)
                .where(LOCAL_BOOKINGS.ID.eq(bookingId))
                .and(LOCAL_BOOKINGS.STATUS.eq(expectedStatus))
                .forUpdate()
                .fetchOne();
    }

    /**
     * Update booking status atomically.
     */
    public int updateStatus(Long bookingId, String newStatus) {
        return dsl.update(LOCAL_BOOKINGS)
                .set(LOCAL_BOOKINGS.STATUS, newStatus)
                .set(LOCAL_BOOKINGS.SYNCED_AT, LocalDateTime.now())
                .where(LOCAL_BOOKINGS.ID.eq(bookingId))
                .execute();
    }

    /**
     * Count pending bookings for a user/game (duplicate booking check).
     */
    public long countByUserAndGameAndStatus(Long userId, Long gameId, String status) {
        return dsl.selectCount()
                .from(LOCAL_BOOKINGS)
                .where(LOCAL_BOOKINGS.USER_ID.eq(userId))
                .and(LOCAL_BOOKINGS.GAME_ID.eq(gameId))
                .and(LOCAL_BOOKINGS.STATUS.eq(status))
                .fetchOne(0, long.class);
    }

    /**
     * Find all bookings for a user.
     */
    public Result<Record> findByUserId(Long userId) {
        return dsl.select()
                .from(LOCAL_BOOKINGS)
                .where(LOCAL_BOOKINGS.USER_ID.eq(userId))
                .orderBy(LOCAL_BOOKINGS.ID.desc())
                .fetch();
    }
}
