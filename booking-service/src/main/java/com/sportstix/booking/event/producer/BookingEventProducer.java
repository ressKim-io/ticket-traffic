package com.sportstix.booking.event.producer;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingSeat;
import com.sportstix.booking.event.outbox.OutboxEventService;
import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.SeatEvent;
import com.sportstix.common.event.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Saves booking/seat events to the outbox table instead of publishing directly.
 * Events are published to Kafka asynchronously by OutboxPollingPublisher.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final OutboxEventService outboxEventService;

    public void publishBookingCreated(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.created(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId(),
                    booking.getTotalPrice());
            outboxEventService.save("Booking", String.valueOf(booking.getId()),
                    "BOOKING_CREATED", Topics.BOOKING_CREATED, key, event);
        }
    }

    public void publishBookingConfirmed(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.confirmed(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId());
            outboxEventService.save("Booking", String.valueOf(booking.getId()),
                    "BOOKING_CONFIRMED", Topics.BOOKING_CONFIRMED, key, event);
        }
    }

    public void publishBookingCancelled(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.cancelled(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId());
            outboxEventService.save("Booking", String.valueOf(booking.getId()),
                    "BOOKING_CANCELLED", Topics.BOOKING_CANCELLED, key, event);
        }
    }

    public void publishSeatsHeld(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            SeatEvent event = SeatEvent.held(
                    booking.getGameId(), seat.getGameSeatId(), booking.getUserId());
            outboxEventService.save("Seat", String.valueOf(seat.getGameSeatId()),
                    "SEAT_HELD", Topics.SEAT_HELD, key, event);
        }
    }

    public void publishSeatsReleased(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            SeatEvent event = SeatEvent.released(
                    booking.getGameId(), seat.getGameSeatId(), booking.getUserId());
            outboxEventService.save("Seat", String.valueOf(seat.getGameSeatId()),
                    "SEAT_RELEASED", Topics.SEAT_RELEASED, key, event);
        }
    }
}
