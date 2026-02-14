package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingSeat;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.domain.LocalGameSeat;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.repository.LocalGameSeatRepository;
import com.sportstix.common.event.SeatEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.booking.event.producer.ResilientKafkaPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    private final ResilientKafkaPublisher kafkaPublisher;
    private final BookingTransactionService transactionService;

    /**
     * Check 1: Find seats stuck in HELD status with no active booking.
     * This can happen when a booking expires but the seat release event was lost.
     * Uses findByStatus() to leverage DB index instead of loading all seats.
     */
    @Transactional
    public int reconcileStaleHeldSeats() {
        List<LocalGameSeat> heldSeats = localGameSeatRepository.findByStatus("HELD");

        if (heldSeats.isEmpty()) return 0;

        // Only PENDING bookings hold seats; CONFIRMED seats are RESERVED, CANCELLED are AVAILABLE
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

                // Notify game-service to sync original seat status
                SeatEvent event = SeatEvent.released(seat.getGameId(), seat.getId(), null);
                kafkaPublisher.publish(Topics.SEAT_RELEASED,
                        String.valueOf(seat.getGameId()), event, "reconcile-seat-released");
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
     * Uses batch loading to avoid N+1 queries.
     */
    @Transactional(readOnly = true)
    public int detectBookingSeatMismatch() {
        List<Booking> confirmedBookings = bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED);
        if (confirmedBookings.isEmpty()) return 0;

        // Batch-load all referenced seats in one query
        Set<Long> seatIds = confirmedBookings.stream()
                .flatMap(b -> b.getBookingSeats().stream())
                .map(BookingSeat::getGameSeatId)
                .collect(Collectors.toSet());
        Map<Long, LocalGameSeat> seatMap = localGameSeatRepository.findAllById(seatIds).stream()
                .collect(Collectors.toMap(LocalGameSeat::getId, s -> s));

        int mismatches = 0;
        for (Booking booking : confirmedBookings) {
            for (BookingSeat bs : booking.getBookingSeats()) {
                LocalGameSeat seat = seatMap.get(bs.getGameSeatId());
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
     * Check 3: Find and cancel expired PENDING bookings that HoldExpiryScheduler missed.
     * Delegates to BookingTransactionService for proper cancellation with Kafka events.
     */
    @Transactional
    public int reconcileExpiredPendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findByStatusAndHoldExpiresAtBefore(
                BookingStatus.PENDING, LocalDateTime.now(), PageRequest.of(0, 200));

        int cancelled = 0;
        for (Booking booking : expiredBookings) {
            try {
                transactionService.releaseBookingById(booking.getId());
                log.warn("RECONCILE: Force-cancelled expired booking {}", booking.getId());
                cancelled++;
            } catch (Exception e) {
                log.error("RECONCILE: Failed to cancel expired booking {}", booking.getId(), e);
            }
        }

        if (cancelled > 0) {
            log.info("RECONCILE: Cancelled {} expired PENDING bookings", cancelled);
        }
        return cancelled;
    }
}
