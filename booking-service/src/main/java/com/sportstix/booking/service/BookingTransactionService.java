package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Record4;
import org.jooq.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static com.sportstix.booking.jooq.LocalGameSeatJooqRepository.*;

/**
 * Separated from BookingService to ensure @Transactional works
 * (avoids Spring AOP self-invocation bypass).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingTransactionService {

    private final BookingRepository bookingRepository;
    private final LocalGameSeatJooqRepository seatJooqRepository;
    private final BookingEventProducer bookingEventProducer;

    /**
     * Tier 2 & 3: DB pessimistic lock + optimistic lock within transaction.
     */
    @Transactional
    public Booking holdSeatsInTransaction(Long userId, Long gameId, Set<Long> gameSeatIds) {
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

    /**
     * Release a booking by ID - loads entity fresh within transaction
     * to avoid LazyInitializationException.
     */
    @Transactional
    public Booking releaseBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == com.sportstix.booking.domain.BookingStatus.CANCELLED) {
            return booking;
        }

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
        log.info("Booking released: bookingId={}", booking.getId());

        return booking;
    }
}
