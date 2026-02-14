package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.repository.LocalGameRepository;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record4;
import org.jooq.Result;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.sportstix.booking.jooq.LocalGameSeatJooqRepository.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int MAX_TICKETS_DEFAULT = 4;

    private final BookingRepository bookingRepository;
    private final LocalGameRepository localGameRepository;
    private final LocalGameSeatJooqRepository seatJooqRepository;
    private final SeatLockService seatLockService;
    private final BookingEventProducer bookingEventProducer;

    /**
     * Hold seats with 3-tier lock:
     * 1. Redis distributed lock (cross-pod)
     * 2. DB pessimistic lock (FOR UPDATE SKIP LOCKED)
     * 3. Optimistic lock (@Version on status change)
     */
    public Booking holdSeats(Long userId, Long gameId, Set<Long> gameSeatIds) {
        log.info("Hold seats: userId={}, gameId={}, seatIds={}", userId, gameId, gameSeatIds);

        // Validate max tickets per user
        var game = localGameRepository.findById(gameId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND,
                        "Game not found: " + gameId));

        int maxTickets = game.getMaxTicketsPerUser() != null
                ? game.getMaxTicketsPerUser() : MAX_TICKETS_DEFAULT;

        long existingCount = bookingRepository.countByUserIdAndGameIdAndStatusIn(
                userId, gameId, List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

        if (existingCount + gameSeatIds.size() > maxTickets) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    "Exceeds max tickets per user: " + maxTickets);
        }

        // Tier 1: Redis distributed lock
        List<RLock> locks = seatLockService.acquireLocks(gameSeatIds);
        try {
            return holdSeatsWithDbLock(userId, gameId, gameSeatIds);
        } finally {
            seatLockService.releaseLocks(locks);
        }
    }

    /**
     * Tier 2 & 3: DB pessimistic lock + optimistic lock within transaction.
     */
    @Transactional
    public Booking holdSeatsWithDbLock(Long userId, Long gameId, Set<Long> gameSeatIds) {
        // Tier 2: DB pessimistic lock (FOR UPDATE SKIP LOCKED)
        Result<Record4<Long, Long, Long, String>> lockedSeats =
                seatJooqRepository.findByIdsForUpdateSkipLocked(gameSeatIds, AVAILABLE);

        if (lockedSeats.size() != gameSeatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    "Some seats are no longer available. Requested=" + gameSeatIds.size()
                            + ", available=" + lockedSeats.size());
        }

        // Create booking
        Booking booking = Booking.builder()
                .userId(userId)
                .gameId(gameId)
                .build();

        for (var seat : lockedSeats) {
            Long seatId = seat.get(0, Long.class);
            Long price = seat.get(2, Long.class);
            booking.addSeat(seatId, BigDecimal.valueOf(price));
        }

        booking = bookingRepository.save(booking);

        // Tier 2 continued: Update seat status (AVAILABLE -> HELD)
        int updated = seatJooqRepository.bulkUpdateStatus(gameSeatIds, AVAILABLE, HELD);
        if (updated != gameSeatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    "Failed to hold all seats. Expected=" + gameSeatIds.size()
                            + ", updated=" + updated);
        }

        log.info("Seats held: bookingId={}, seats={}", booking.getId(), gameSeatIds);

        // Publish events
        bookingEventProducer.publishBookingCreated(booking);
        bookingEventProducer.publishSeatsHeld(booking);

        return booking;
    }

    @Transactional
    public Booking confirmBooking(Long bookingId) {
        log.info("Confirming booking: bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (booking.isExpired()) {
            throw new BusinessException(ErrorCode.BOOKING_EXPIRED,
                    "Booking has expired: " + bookingId);
        }

        Set<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getGameSeatId())
                .collect(java.util.stream.Collectors.toSet());

        // Update seats HELD -> RESERVED
        int updated = seatJooqRepository.bulkUpdateStatus(seatIds, HELD, RESERVED);
        if (updated != seatIds.size()) {
            log.warn("Partial seat reservation: bookingId={}, expected={}, updated={}",
                    bookingId, seatIds.size(), updated);
        }

        booking.confirm();
        booking = bookingRepository.save(booking);

        bookingEventProducer.publishBookingConfirmed(booking);
        log.info("Booking confirmed: bookingId={}", bookingId);

        return booking;
    }

    @Transactional
    public Booking cancelBooking(Long bookingId, Long userId) {
        log.info("Cancelling booking: bookingId={}, userId={}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User does not own booking: " + bookingId);
        }

        return releaseBooking(booking);
    }

    @Transactional
    public Booking releaseBooking(Booking booking) {
        Set<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getGameSeatId())
                .collect(java.util.stream.Collectors.toSet());

        // Release seats back to AVAILABLE (from HELD or RESERVED)
        seatJooqRepository.bulkUpdateStatus(seatIds, HELD, AVAILABLE);
        seatJooqRepository.bulkUpdateStatus(seatIds, RESERVED, AVAILABLE);

        booking.cancel();
        booking = bookingRepository.save(booking);

        bookingEventProducer.publishBookingCancelled(booking);
        bookingEventProducer.publishSeatsReleased(booking);
        log.info("Booking cancelled: bookingId={}", booking.getId());

        return booking;
    }

    @Transactional(readOnly = true)
    public Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));
    }
}
