package com.sportstix.booking.event.producer;

import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.SeatEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBookingCreated(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.created(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId(),
                    booking.getTotalPrice());
            publish(Topics.BOOKING_CREATED, key, event, "booking-created");
        }
    }

    public void publishBookingConfirmed(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.confirmed(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId());
            publish(Topics.BOOKING_CONFIRMED, key, event, "booking-confirmed");
        }
    }

    public void publishBookingCancelled(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            BookingEvent event = BookingEvent.cancelled(
                    booking.getId(), booking.getUserId(),
                    booking.getGameId(), seat.getGameSeatId());
            publish(Topics.BOOKING_CANCELLED, key, event, "booking-cancelled");
        }
    }

    public void publishSeatsHeld(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            SeatEvent event = SeatEvent.held(
                    booking.getGameId(), seat.getGameSeatId(), booking.getUserId());
            publish(Topics.SEAT_HELD, key, event, "seat-held");
        }
    }

    public void publishSeatsReleased(Booking booking) {
        String key = String.valueOf(booking.getGameId());
        for (BookingSeat seat : booking.getBookingSeats()) {
            SeatEvent event = SeatEvent.released(
                    booking.getGameId(), seat.getGameSeatId(), booking.getUserId());
            publish(Topics.SEAT_RELEASED, key, event, "seat-released");
        }
    }

    private void publish(String topic, String key, Object event, String eventName) {
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {}: topic={}", eventName, topic, ex);
                    }
                });
    }
}
