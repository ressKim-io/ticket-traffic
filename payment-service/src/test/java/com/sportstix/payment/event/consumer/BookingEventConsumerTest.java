package com.sportstix.payment.event.consumer;

import com.sportstix.common.event.BookingEvent;
import com.sportstix.payment.domain.LocalBooking;
import com.sportstix.payment.repository.LocalBookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BookingEventConsumerTest {

    @InjectMocks
    private BookingEventConsumer consumer;

    @Mock
    private LocalBookingRepository localBookingRepository;

    @Test
    void handleBookingCreated_createsLocalBooking() {
        given(localBookingRepository.existsById(1L)).willReturn(false);
        BookingEvent event = BookingEvent.created(1L, 100L, 10L, 50L);

        consumer.handleBookingCreated(event);

        ArgumentCaptor<LocalBooking> captor = ArgumentCaptor.forClass(LocalBooking.class);
        verify(localBookingRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(100L);
        assertThat(captor.getValue().getGameId()).isEqualTo(10L);
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void handleBookingCreated_duplicate_ignored() {
        given(localBookingRepository.existsById(1L)).willReturn(true);
        BookingEvent event = BookingEvent.created(1L, 100L, 10L, 50L);

        consumer.handleBookingCreated(event);

        verify(localBookingRepository, never()).save(any());
    }

    @Test
    void handleBookingConfirmed_updatesStatus() {
        LocalBooking existing = new LocalBooking(1L, 100L, 10L, "PENDING", null);
        given(localBookingRepository.findById(1L)).willReturn(Optional.of(existing));

        BookingEvent event = BookingEvent.confirmed(1L, 100L, 10L, 50L);
        consumer.handleBookingConfirmed(event);

        verify(localBookingRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void handleBookingConfirmed_notFound_logsWarning() {
        given(localBookingRepository.findById(1L)).willReturn(Optional.empty());

        BookingEvent event = BookingEvent.confirmed(1L, 100L, 10L, 50L);
        consumer.handleBookingConfirmed(event);

        verify(localBookingRepository, never()).save(any());
    }

    @Test
    void handleBookingCancelled_updatesStatus() {
        LocalBooking existing = new LocalBooking(1L, 100L, 10L, "PENDING", null);
        given(localBookingRepository.findById(1L)).willReturn(Optional.of(existing));

        BookingEvent event = BookingEvent.cancelled(1L, 100L, 10L, 50L);
        consumer.handleBookingCancelled(event);

        verify(localBookingRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
    }
}
