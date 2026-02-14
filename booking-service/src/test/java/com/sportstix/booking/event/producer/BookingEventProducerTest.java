package com.sportstix.booking.event.producer;

import com.sportstix.booking.TestFixtures;
import com.sportstix.booking.domain.Booking;
import com.sportstix.common.event.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BookingEventProducerTest {

    @Mock
    private ResilientKafkaPublisher publisher;

    @InjectMocks
    private BookingEventProducer bookingEventProducer;

    @Test
    void publishBookingCreated_publishesPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));
        booking.addSeat(201L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingCreated(booking);

        verify(publisher, times(2)).publish(
                eq(Topics.BOOKING_CREATED), eq("10"), any(), eq("booking-created"));
    }

    @Test
    void publishBookingConfirmed_publishesPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingConfirmed(booking);

        verify(publisher).publish(
                eq(Topics.BOOKING_CONFIRMED), eq("10"), any(), eq("booking-confirmed"));
    }

    @Test
    void publishBookingCancelled_publishesPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingCancelled(booking);

        verify(publisher).publish(
                eq(Topics.BOOKING_CANCELLED), eq("10"), any(), eq("booking-cancelled"));
    }

    @Test
    void publishSeatsHeld_publishesPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));
        booking.addSeat(201L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishSeatsHeld(booking);

        verify(publisher, times(2)).publish(
                eq(Topics.SEAT_HELD), eq("10"), any(), eq("seat-held"));
    }

    @Test
    void publishSeatsReleased_publishesPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishSeatsReleased(booking);

        verify(publisher).publish(
                eq(Topics.SEAT_RELEASED), eq("10"), any(), eq("seat-released"));
    }

    private Booking createBooking(Long id, Long userId, Long gameId) {
        return TestFixtures.createBookingWithId(id, userId, gameId);
    }
}
