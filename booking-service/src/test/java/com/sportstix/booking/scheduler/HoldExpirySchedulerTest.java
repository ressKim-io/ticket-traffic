package com.sportstix.booking.scheduler;

import com.sportstix.booking.TestFixtures;
import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.service.BookingTransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HoldExpirySchedulerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingTransactionService transactionService;

    @InjectMocks
    private HoldExpiryScheduler scheduler;

    @Test
    void releaseExpiredHolds_noExpiredBookings_doesNothing() {
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        scheduler.releaseExpiredHolds();

        verify(transactionService, never()).releaseBookingById(any());
    }

    @Test
    void releaseExpiredHolds_withExpiredBookings_releasesEach() {
        Booking expired1 = createBooking(1L);
        Booking expired2 = createBooking(2L);

        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(expired1, expired2));

        scheduler.releaseExpiredHolds();

        verify(transactionService).releaseBookingById(1L);
        verify(transactionService).releaseBookingById(2L);
    }

    @Test
    void releaseExpiredHolds_oneFailsOtherContinues() {
        Booking expired1 = createBooking(1L);
        Booking expired2 = createBooking(2L);

        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(expired1, expired2));
        when(transactionService.releaseBookingById(1L))
                .thenThrow(new RuntimeException("DB error"));
        when(transactionService.releaseBookingById(2L))
                .thenReturn(expired2);

        scheduler.releaseExpiredHolds();

        // Both should be attempted even though first failed
        verify(transactionService).releaseBookingById(1L);
        verify(transactionService).releaseBookingById(2L);
    }

    private Booking createBooking(Long id) {
        Booking booking = TestFixtures.createBookingWithId(id, 1L, 10L);
        booking.addSeat(100L + id, BigDecimal.valueOf(50000));
        return booking;
    }
}
