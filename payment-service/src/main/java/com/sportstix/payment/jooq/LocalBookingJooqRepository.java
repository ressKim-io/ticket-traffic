package com.sportstix.payment.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static com.sportstix.payment.jooq.generated.Tables.LOCAL_BOOKINGS;

/**
 * jOOQ repository for hot path booking queries in payment-service.
 * Handles: payment eligibility checks with locking, atomic status updates.
 * All locking methods MUST be called within an active transaction.
 */
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocalBookingJooqRepository {

    private final DSLContext dsl;

    /**
     * Find booking with FOR UPDATE lock for payment processing.
     */
    public Record3<Long, Long, String> findByIdForUpdate(Long bookingId, String expectedStatus) {
        return dsl.select(
                        LOCAL_BOOKINGS.ID,
                        LOCAL_BOOKINGS.USER_ID,
                        LOCAL_BOOKINGS.STATUS)
                .from(LOCAL_BOOKINGS)
                .where(LOCAL_BOOKINGS.ID.eq(bookingId))
                .and(LOCAL_BOOKINGS.STATUS.eq(expectedStatus))
                .forUpdate()
                .fetchOne();
    }

    /**
     * Update booking status atomically.
     */
    @Transactional
    public int updateStatus(Long bookingId, String newStatus) {
        return dsl.update(LOCAL_BOOKINGS)
                .set(LOCAL_BOOKINGS.STATUS, newStatus)
                .set(LOCAL_BOOKINGS.SYNCED_AT, DSL.currentLocalDateTime())
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
}
