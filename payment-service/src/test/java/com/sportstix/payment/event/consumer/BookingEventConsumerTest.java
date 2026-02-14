package com.sportstix.payment.event.consumer;

import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.payment.domain.LocalBooking;
import com.sportstix.payment.event.IdempotencyService;
import com.sportstix.payment.repository.LocalBookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @InjectMocks
    private BookingEventConsumer consumer;

    @Mock
    private LocalBookingRepository localBookingRepository;
    @Mock
    private IdempotencyService idempotencyService;

    @Test
    void handleBookingCreated_createsLocalBooking() {
        BookingEvent event = BookingEvent.created(1L, 100L, 10L, 50L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CREATED)).willReturn(false);

        consumer.handleBookingCreated(event);

        ArgumentCaptor<LocalBooking> captor = ArgumentCaptor.forClass(LocalBooking.class);
        verify(localBookingRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getGameId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getTotalPrice()).isEqualTo(BigDecimal.valueOf(50000));
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.BOOKING_CREATED);
    }

    @Test
    void handleBookingCreated_duplicate_skipped() {
        BookingEvent event = BookingEvent.created(1L, 100L, 10L, 50L, BigDecimal.valueOf(50000));
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CREATED)).willReturn(true);

        consumer.handleBookingCreated(event);

        verify(localBookingRepository, never()).save(any());
        verify(idempotencyService, never()).markProcessed(any(), any());
    }

    @Test
    void handleBookingConfirmed_updatesStatus() {
        LocalBooking existing = new LocalBooking(1L, 100L, 10L, "PENDING", null);
        BookingEvent event = BookingEvent.confirmed(1L, 100L, 10L, 50L);
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CONFIRMED)).willReturn(false);
        given(localBookingRepository.findById(1L)).willReturn(Optional.of(existing));

        consumer.handleBookingConfirmed(event);

        verify(localBookingRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CONFIRMED");
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.BOOKING_CONFIRMED);
    }

    @Test
    void handleBookingConfirmed_notFound_logsWarning() {
        BookingEvent event = BookingEvent.confirmed(1L, 100L, 10L, 50L);
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CONFIRMED)).willReturn(false);
        given(localBookingRepository.findById(1L)).willReturn(Optional.empty());

        consumer.handleBookingConfirmed(event);

        verify(localBookingRepository, never()).save(any());
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.BOOKING_CONFIRMED);
    }

    @Test
    void handleBookingCancelled_updatesStatus() {
        LocalBooking existing = new LocalBooking(1L, 100L, 10L, "PENDING", null);
        BookingEvent event = BookingEvent.cancelled(1L, 100L, 10L, 50L);
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CANCELLED)).willReturn(false);
        given(localBookingRepository.findById(1L)).willReturn(Optional.of(existing));

        consumer.handleBookingCancelled(event);

        verify(localBookingRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
        verify(idempotencyService).markProcessed(event.getEventId(), Topics.BOOKING_CANCELLED);
    }

    @Test
    void handleBookingConfirmed_duplicate_skipped() {
        BookingEvent event = BookingEvent.confirmed(1L, 100L, 10L, 50L);
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CONFIRMED)).willReturn(true);

        consumer.handleBookingConfirmed(event);

        verify(localBookingRepository, never()).findById(anyLong());
        verify(localBookingRepository, never()).save(any());
    }

    @Test
    void handleBookingCancelled_duplicate_skipped() {
        BookingEvent event = BookingEvent.cancelled(1L, 100L, 10L, 50L);
        given(idempotencyService.isDuplicate(event.getEventId(), Topics.BOOKING_CANCELLED)).willReturn(true);

        consumer.handleBookingCancelled(event);

        verify(localBookingRepository, never()).findById(anyLong());
        verify(localBookingRepository, never()).save(any());
    }
}
