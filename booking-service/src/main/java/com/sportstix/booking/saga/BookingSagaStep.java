package com.sportstix.booking.saga;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static com.sportstix.booking.jooq.LocalGameSeatJooqRepository.HELD;
import static com.sportstix.booking.jooq.LocalGameSeatJooqRepository.RESERVED;

/**
 * SAGA step: confirms booking after successful payment.
 * Updates seat status from HELD to RESERVED and booking status to CONFIRMED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingSagaStep {

    private final BookingRepository bookingRepository;
    private final LocalGameSeatJooqRepository seatJooqRepository;
    private final BookingEventProducer bookingEventProducer;

    @Transactional
    public void confirm(Long bookingId) {
        log.info("SAGA confirm: bookingId={}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND,
                        "Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            log.info("SAGA confirm skipped (already confirmed): bookingId={}", bookingId);
            return;
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("SAGA confirm skipped (invalid state): bookingId={}, status={}",
                    bookingId, booking.getStatus());
            return;
        }

        if (booking.isExpired()) {
            throw new BusinessException(ErrorCode.BOOKING_EXPIRED,
                    "Booking has expired: " + bookingId);
        }

        Set<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getGameSeatId())
                .collect(Collectors.toSet());

        int updated = seatJooqRepository.bulkUpdateStatus(seatIds, HELD, RESERVED);
        if (updated != seatIds.size()) {
            throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE,
                    "Failed to reserve seats: expected=" + seatIds.size()
                            + ", updated=" + updated);
        }

        booking.confirm();
        bookingRepository.save(booking);

        bookingEventProducer.publishBookingConfirmed(booking);
        log.info("SAGA confirm completed: bookingId={}", bookingId);
    }
}
