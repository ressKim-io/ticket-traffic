package com.sportstix.booking.scheduler;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.service.BookingTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler to release expired HELD seats (5-minute hold TTL).
 * Runs every 30 seconds to clean up expired bookings.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryScheduler {

    private static final int BATCH_SIZE = 100;

    private final BookingRepository bookingRepository;
    private final BookingTransactionService transactionService;

    @Scheduled(fixedRate = 30_000)
    public void releaseExpiredHolds() {
        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndHoldExpiresAtBefore(
                        BookingStatus.PENDING, LocalDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE));

        if (expiredBookings.isEmpty()) {
            return;
        }

        log.info("Releasing {} expired bookings", expiredBookings.size());

        for (Booking booking : expiredBookings) {
            try {
                transactionService.releaseBookingById(booking.getId());
                log.info("Released expired booking: bookingId={}", booking.getId());
            } catch (Exception e) {
                log.error("Failed to release expired booking: bookingId={}",
                        booking.getId(), e);
            }
        }
    }
}
