package com.sportstix.booking.saga;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSagaStepTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private LocalGameSeatJooqRepository seatJooqRepository;
    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingSagaStep bookingSagaStep;

    @Test
    void confirm_success_reservesSeatsAndConfirms() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.addSeat(1L, BigDecimal.valueOf(50000));
        booking.addSeat(2L, BigDecimal.valueOf(50000));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(seatJooqRepository.bulkUpdateStatus(any(), eq("HELD"), eq("RESERVED"))).thenReturn(2);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        bookingSagaStep.confirm(1L);

        verify(seatJooqRepository).bulkUpdateStatus(any(), eq("HELD"), eq("RESERVED"));
        verify(bookingEventProducer).publishBookingConfirmed(any());
    }

    @Test
    void confirm_alreadyConfirmed_skips() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.confirm();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        bookingSagaStep.confirm(1L);

        verify(seatJooqRepository, never()).bulkUpdateStatus(any(), any(), any());
        verify(bookingEventProducer, never()).publishBookingConfirmed(any());
    }

    @Test
    void confirm_cancelled_skips() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.cancel();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        bookingSagaStep.confirm(1L);

        verify(seatJooqRepository, never()).bulkUpdateStatus(any(), any(), any());
    }

    @Test
    void confirm_notFound_throwsException() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingSagaStep.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Booking not found");
    }

    @Test
    void confirm_partialSeatUpdate_throwsException() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.addSeat(1L, BigDecimal.valueOf(50000));
        booking.addSeat(2L, BigDecimal.valueOf(50000));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(seatJooqRepository.bulkUpdateStatus(any(), eq("HELD"), eq("RESERVED"))).thenReturn(1);

        assertThatThrownBy(() -> bookingSagaStep.confirm(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to reserve seats");
    }
}
