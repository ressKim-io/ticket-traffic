package com.sportstix.payment.event.consumer;

import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.payment.domain.LocalBooking;
import com.sportstix.payment.repository.LocalBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumes booking events to sync local_bookings replica in payment_db.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventConsumer {

    private final LocalBookingRepository localBookingRepository;

    @KafkaListener(topics = Topics.BOOKING_CREATED, groupId = "payment-service")
    @Transactional
    public void handleBookingCreated(BookingEvent event) {
        log.info("Received booking-created event: bookingId={}, userId={}, gameId={}",
                event.getBookingId(), event.getUserId(), event.getGameId());

        LocalBooking localBooking = new LocalBooking(
                event.getBookingId(),
                event.getUserId(),
                event.getGameId(),
                "PENDING",
                null
        );
        localBookingRepository.save(localBooking);
        log.info("Created local booking replica: bookingId={}", event.getBookingId());
    }

    @KafkaListener(topics = Topics.BOOKING_CONFIRMED, groupId = "payment-service")
    @Transactional
    public void handleBookingConfirmed(BookingEvent event) {
        log.info("Received booking-confirmed event: bookingId={}", event.getBookingId());

        localBookingRepository.findById(event.getBookingId())
                .ifPresentOrElse(
                        booking -> {
                            booking.updateStatus("CONFIRMED");
                            localBookingRepository.save(booking);
                        },
                        () -> log.warn("Local booking not found for confirmed event: bookingId={}",
                                event.getBookingId())
                );
    }

    @KafkaListener(topics = Topics.BOOKING_CANCELLED, groupId = "payment-service")
    @Transactional
    public void handleBookingCancelled(BookingEvent event) {
        log.info("Received booking-cancelled event: bookingId={}", event.getBookingId());

        localBookingRepository.findById(event.getBookingId())
                .ifPresentOrElse(
                        booking -> {
                            booking.updateStatus("CANCELLED");
                            localBookingRepository.save(booking);
                        },
                        () -> log.warn("Local booking not found for cancelled event: bookingId={}",
                                event.getBookingId())
                );
    }
}
