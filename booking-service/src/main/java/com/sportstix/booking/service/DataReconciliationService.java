package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingSeat;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.domain.LocalGameSeat;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.repository.LocalGameSeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates data consistency between bookings and local game seat replicas.
 * Detects and auto-corrects stale HELD seats and orphaned reservations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataReconciliationService {

    private final BookingRepository bookingRepository;
    private final LocalGameSeatRepository localGameSeatRepository;

    /**
     * Check 1: Find seats stuck in HELD status with no active booking.
     * This can happen when a booking expires but the seat release event was lost.
     */
    @Transactional
    public int reconcileStaleHeldSeats() {
        List<LocalGameSeat> heldSeats = localGameSeatRepository.findAll().stream()
                .filter(s -> "HELD".equals(s.getStatus()))
                .toList();

        if (heldSeats.isEmpty()) return 0;

        // Get all PENDING bookings and their held seat IDs
        List<Booking> pendingBookings = bookingRepository.findByStatusWithSeats(BookingStatus.PENDING);
        Set<Long> activeHeldSeatIds = pendingBookings.stream()
                .flatMap(b -> b.getBookingSeats().stream())
                .map(BookingSeat::getGameSeatId)
                .collect(Collectors.toSet());

        int released = 0;
        for (LocalGameSeat seat : heldSeats) {
            if (!activeHeldSeatIds.contains(seat.getId())) {
                log.warn("RECONCILE: Releasing stale HELD seat id={}, gameId={}",
                        seat.getId(), seat.getGameId());
                seat.release();
                localGameSeatRepository.save(seat);
                released++;
            }
        }

        if (released > 0) {
            log.info("RECONCILE: Released {} stale HELD seats", released);
        }
        return released;
    }

    /**
     * Check 2: Find bookings in CONFIRMED status whose seats are not RESERVED.
     * This can happen when the seat status update event was lost after payment.
     */
    @Transactional(readOnly = true)
    public int detectBookingSeatMismatch() {
        List<Booking> confirmedBookings = bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED);
        int mismatches = 0;

        for (Booking booking : confirmedBookings) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                LocalGameSeat seat = localGameSeatRepository.findById(bs.getGameSeatId()).orElse(null);
                if (seat == null) {
                    log.warn("RECONCILE: Confirmed booking {} references missing seat id={}",
                            booking.getId(), bs.getGameSeatId());
                    mismatches++;
                } else if (!"RESERVED".equals(seat.getStatus())) {
                    log.warn("RECONCILE: Confirmed booking {} seat id={} has status={} (expected RESERVED)",
                            booking.getId(), bs.getGameSeatId(), seat.getStatus());
                    mismatches++;
                }
            }
        }

        if (mismatches > 0) {
            log.warn("RECONCILE: Found {} booking-seat status mismatches", mismatches);
        }
        return mismatches;
    }

    /**
     * Check 3: Find expired PENDING bookings that should have been cancelled.
     * Detects bookings whose hold period has passed without confirmation.
     */
    @Transactional(readOnly = true)
    public int detectExpiredPendingBookings() {
        List<Booking> pendingBookings = bookingRepository.findByStatusWithSeats(BookingStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;

        for (Booking booking : pendingBookings) {
            if (booking.getHoldExpiresAt() != null && booking.getHoldExpiresAt().isBefore(now)) {
                log.warn("RECONCILE: Booking {} is PENDING but hold expired at {}",
                        booking.getId(), booking.getHoldExpiresAt());
                expired++;
            }
        }

        if (expired > 0) {
            log.warn("RECONCILE: Found {} expired PENDING bookings", expired);
        }
        return expired;
    }

    /**
     * Run all reconciliation checks and return a summary.
     */
    public Map<String, Integer> runAll() {
        int staleSeats = reconcileStaleHeldSeats();
        int seatMismatches = detectBookingSeatMismatch();
        int expiredBookings = detectExpiredPendingBookings();

        return Map.of(
                "staleHeldSeatsReleased", staleSeats,
                "bookingSeatMismatches", seatMismatches,
                "expiredPendingBookings", expiredBookings
        );
    }
}
