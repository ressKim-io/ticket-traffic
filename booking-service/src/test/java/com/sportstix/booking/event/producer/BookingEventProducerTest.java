package com.sportstix.booking.event.producer;

import com.sportstix.booking.TestFixtures;
import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.event.outbox.OutboxEventService;
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
    private OutboxEventService outboxEventService;

    @InjectMocks
    private BookingEventProducer bookingEventProducer;

    @Test
    void publishBookingCreated_savesToOutboxPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));
        booking.addSeat(201L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingCreated(booking);

        verify(outboxEventService, times(2)).save(
                eq("Booking"), eq("1"),
                eq("BOOKING_CREATED"), eq(Topics.BOOKING_CREATED),
                eq("10"), any());
    }

    @Test
    void publishBookingConfirmed_savesToOutboxPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingConfirmed(booking);

        verify(outboxEventService).save(
                eq("Booking"), eq("1"),
                eq("BOOKING_CONFIRMED"), eq(Topics.BOOKING_CONFIRMED),
                eq("10"), any());
    }

    @Test
    void publishBookingCancelled_savesToOutboxPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishBookingCancelled(booking);

        verify(outboxEventService).save(
                eq("Booking"), eq("1"),
                eq("BOOKING_CANCELLED"), eq(Topics.BOOKING_CANCELLED),
                eq("10"), any());
    }

    @Test
    void publishSeatsHeld_savesToOutboxPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));
        booking.addSeat(201L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishSeatsHeld(booking);

        verify(outboxEventService, times(2)).save(
                eq("Seat"), anyString(),
                eq("SEAT_HELD"), eq(Topics.SEAT_HELD),
                eq("10"), any());
    }

    @Test
    void publishSeatsReleased_savesToOutboxPerSeat() {
        Booking booking = createBooking(1L, 100L, 10L);
        booking.addSeat(200L, BigDecimal.valueOf(50000));

        bookingEventProducer.publishSeatsReleased(booking);

        verify(outboxEventService).save(
                eq("Seat"), anyString(),
                eq("SEAT_RELEASED"), eq(Topics.SEAT_RELEASED),
                eq("10"), any());
    }

    private Booking createBooking(Long id, Long userId, Long gameId) {
        return TestFixtures.createBookingWithId(id, userId, gameId);
    }
}
