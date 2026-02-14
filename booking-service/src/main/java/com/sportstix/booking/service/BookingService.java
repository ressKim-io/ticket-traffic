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
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final BookingTransactionService transactionService;

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
            // Tier 2 & 3: called via proxy (separate bean) to ensure @Transactional works
            return transactionService.holdSeatsInTransaction(userId, gameId, gameSeatIds);
        } finally {
            seatLockService.releaseLocks(locks);
        }
    }

    @Transactional
    public Booking confirmBooking(Long bookingId, Long userId) {
        log.info("Confirming booking: bookingId={}, userId={}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User does not own booking: " + bookingId);
        }

        if (booking.isExpired()) {
            throw new BusinessException(ErrorCode.BOOKING_EXPIRED,
                    "Booking has expired: " + bookingId);
        }

        Set<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getGameSeatId())
                .collect(Collectors.toSet());

        // Update seats HELD -> RESERVED
        int updated = seatJooqRepository.bulkUpdateStatus(seatIds, HELD, RESERVED);
        if (updated != seatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    "Failed to reserve all seats. Expected=" + seatIds.size()
                            + ", updated=" + updated);
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

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        Set<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getGameSeatId())
                .collect(Collectors.toSet());

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
    public Booking getBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN,
                    "User does not own booking: " + bookingId);
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
