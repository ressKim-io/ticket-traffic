package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.domain.LocalGameSeat;
import com.sportstix.booking.event.producer.ResilientKafkaPublisher;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.repository.LocalGameSeatRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataReconciliationServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private LocalGameSeatRepository localGameSeatRepository;
    @Mock
    private ResilientKafkaPublisher kafkaPublisher;
    @Mock
    private BookingTransactionService transactionService;

    @InjectMocks
    private DataReconciliationService reconciliationService;

    // ---- reconcileStaleHeldSeats ----

    @Test
    void reconcileStaleHeldSeats_noHeldSeats_returnsZero() {
        when(localGameSeatRepository.findByStatus("HELD")).thenReturn(Collections.emptyList());

        int result = reconciliationService.reconcileStaleHeldSeats();

        assertThat(result).isZero();
        verify(localGameSeatRepository, never()).save(any());
    }

    @Test
    void reconcileStaleHeldSeats_seatWithActiveBooking_notReleased() {
        LocalGameSeat heldSeat = createSeat(100L, 10L, "HELD");
        Booking pendingBooking = createBookingWithSeat(1L, 100L, BookingStatus.PENDING);

        when(localGameSeatRepository.findByStatus("HELD")).thenReturn(List.of(heldSeat));
        when(bookingRepository.findByStatusWithSeats(BookingStatus.PENDING)).thenReturn(List.of(pendingBooking));

        int result = reconciliationService.reconcileStaleHeldSeats();

        assertThat(result).isZero();
        verify(localGameSeatRepository, never()).save(any());
        verify(kafkaPublisher, never()).publish(any(), any(), any(), any());
    }

    @Test
    void reconcileStaleHeldSeats_orphanedSeat_releasedAndEventPublished() {
        LocalGameSeat heldSeat = createSeat(100L, 10L, "HELD");

        when(localGameSeatRepository.findByStatus("HELD")).thenReturn(List.of(heldSeat));
        when(bookingRepository.findByStatusWithSeats(BookingStatus.PENDING)).thenReturn(Collections.emptyList());

        int result = reconciliationService.reconcileStaleHeldSeats();

        assertThat(result).isEqualTo(1);
        assertThat(heldSeat.getStatus()).isEqualTo("AVAILABLE");
        verify(localGameSeatRepository).save(heldSeat);
        verify(kafkaPublisher).publish(eq("ticket.seat.released"), eq("10"), any(), eq("reconcile-seat-released"));
    }

    @Test
    void reconcileStaleHeldSeats_mixedSeats_onlyOrphanedReleased() {
        LocalGameSeat orphanedSeat = createSeat(100L, 10L, "HELD");
        LocalGameSeat activeSeat = createSeat(200L, 10L, "HELD");
        Booking pendingBooking = createBookingWithSeat(1L, 200L, BookingStatus.PENDING);

        when(localGameSeatRepository.findByStatus("HELD")).thenReturn(List.of(orphanedSeat, activeSeat));
        when(bookingRepository.findByStatusWithSeats(BookingStatus.PENDING)).thenReturn(List.of(pendingBooking));

        int result = reconciliationService.reconcileStaleHeldSeats();

        assertThat(result).isEqualTo(1);
        assertThat(orphanedSeat.getStatus()).isEqualTo("AVAILABLE");
        assertThat(activeSeat.getStatus()).isEqualTo("HELD");
        verify(localGameSeatRepository, times(1)).save(orphanedSeat);
        verify(localGameSeatRepository, never()).save(activeSeat);
    }

    // ---- detectBookingSeatMismatch ----

    @Test
    void detectBookingSeatMismatch_noConfirmedBookings_returnsZero() {
        when(bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED)).thenReturn(Collections.emptyList());

        int result = reconciliationService.detectBookingSeatMismatch();

        assertThat(result).isZero();
    }

    @Test
    void detectBookingSeatMismatch_allSeatsReserved_returnsZero() {
        Booking booking = createBookingWithSeat(1L, 100L, BookingStatus.CONFIRMED);
        LocalGameSeat seat = createSeat(100L, 10L, "RESERVED");

        when(bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(localGameSeatRepository.findAllById(anyCollection())).thenReturn(List.of(seat));

        int result = reconciliationService.detectBookingSeatMismatch();

        assertThat(result).isZero();
    }

    @Test
    void detectBookingSeatMismatch_seatNotReserved_detectedAsMismatch() {
        Booking booking = createBookingWithSeat(1L, 100L, BookingStatus.CONFIRMED);
        LocalGameSeat seat = createSeat(100L, 10L, "AVAILABLE");

        when(bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(localGameSeatRepository.findAllById(anyCollection())).thenReturn(List.of(seat));

        int result = reconciliationService.detectBookingSeatMismatch();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void detectBookingSeatMismatch_missingSeat_detectedAsMismatch() {
        Booking booking = createBookingWithSeat(1L, 100L, BookingStatus.CONFIRMED);

        when(bookingRepository.findByStatusWithSeats(BookingStatus.CONFIRMED)).thenReturn(List.of(booking));
        when(localGameSeatRepository.findAllById(anyCollection())).thenReturn(Collections.emptyList());

        int result = reconciliationService.detectBookingSeatMismatch();

        assertThat(result).isEqualTo(1);
    }

    // ---- reconcileExpiredPendingBookings ----

    @Test
    void reconcileExpiredPendingBookings_noExpired_returnsZero() {
        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        int result = reconciliationService.reconcileExpiredPendingBookings();

        assertThat(result).isZero();
        verify(transactionService, never()).releaseBookingById(any());
    }

    @Test
    void reconcileExpiredPendingBookings_expiredBooking_cancelled() {
        Booking expired = Booking.builder().userId(1L).gameId(10L).build();
        // Use reflection to set ID for testing
        setId(expired, 99L);

        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(expired));
        when(transactionService.releaseBookingById(99L)).thenReturn(expired);

        int result = reconciliationService.reconcileExpiredPendingBookings();

        assertThat(result).isEqualTo(1);
        verify(transactionService).releaseBookingById(99L);
    }

    @Test
    void reconcileExpiredPendingBookings_cancelFails_countsAsZero() {
        Booking expired = Booking.builder().userId(1L).gameId(10L).build();
        setId(expired, 99L);

        when(bookingRepository.findByStatusAndHoldExpiresAtBefore(
                eq(BookingStatus.PENDING), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(expired));
        when(transactionService.releaseBookingById(99L))
                .thenThrow(new RuntimeException("DB error"));

        int result = reconciliationService.reconcileExpiredPendingBookings();

        // Error is caught, but not counted as cancelled
        assertThat(result).isZero();
    }

    // ---- Helpers ----

    private LocalGameSeat createSeat(Long id, Long gameId, String status) {
        LocalGameSeat seat = new LocalGameSeat(id, gameId, id, 1L,
                BigDecimal.valueOf(50000), "A", 1);
        // Set to HELD/RESERVED via domain methods
        if ("HELD".equals(status)) {
            seat.hold();
        } else if ("RESERVED".equals(status)) {
            seat.hold();
            seat.reserve();
        }
        return seat;
    }

    private Booking createBookingWithSeat(Long bookingId, Long seatId, BookingStatus targetStatus) {
        Booking booking = Booking.builder().userId(1L).gameId(10L).build();
        booking.addSeat(seatId, BigDecimal.valueOf(50000));
        setId(booking, bookingId);
        if (targetStatus == BookingStatus.CONFIRMED) {
            booking.confirm();
        } else if (targetStatus == BookingStatus.CANCELLED) {
            booking.cancel();
        }
        return booking;
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
