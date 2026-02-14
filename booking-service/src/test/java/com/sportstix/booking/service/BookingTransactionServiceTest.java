package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.common.exception.BusinessException;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingTransactionServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private LocalGameSeatJooqRepository seatJooqRepository;
    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingTransactionService transactionService;

    @Test
    void holdSeatsInTransaction_success_createsBooking() {
        Long userId = 100L;
        Long gameId = 10L;
        Set<Long> seatIds = Set.of(1L, 2L);

        var dsl = DSL.using(SQLDialect.DEFAULT);
        Result<Record4<Long, Long, Long, String>> result = dsl.newResult(
                DSL.field("id", Long.class),
                DSL.field("game_id", Long.class),
                DSL.field("price", Long.class),
                DSL.field("status", String.class));
        result.add(dsl.newRecord(
                DSL.field("id", Long.class),
                DSL.field("game_id", Long.class),
                DSL.field("price", Long.class),
                DSL.field("status", String.class))
                .values(1L, gameId, 50000L, "AVAILABLE"));
        result.add(dsl.newRecord(
                DSL.field("id", Long.class),
                DSL.field("game_id", Long.class),
                DSL.field("price", Long.class),
                DSL.field("status", String.class))
                .values(2L, gameId, 50000L, "AVAILABLE"));

        when(seatJooqRepository.findByIdsForUpdateSkipLocked(seatIds, "AVAILABLE")).thenReturn(result);
        when(seatJooqRepository.bulkUpdateStatus(seatIds, "AVAILABLE", "HELD")).thenReturn(2);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = transactionService.holdSeatsInTransaction(userId, gameId, seatIds);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalPrice()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(booking.getBookingSeats()).hasSize(2);
        verify(bookingEventProducer).publishBookingCreated(any());
        verify(bookingEventProducer).publishSeatsHeld(any());
    }

    @Test
    void holdSeatsInTransaction_seatsNotAvailable_throwsException() {
        Long userId = 100L;
        Long gameId = 10L;
        Set<Long> seatIds = Set.of(1L, 2L);

        var dsl = DSL.using(SQLDialect.DEFAULT);
        Result<Record4<Long, Long, Long, String>> result = dsl.newResult(
                DSL.field("id", Long.class),
                DSL.field("game_id", Long.class),
                DSL.field("price", Long.class),
                DSL.field("status", String.class));
        result.add(dsl.newRecord(
                DSL.field("id", Long.class),
                DSL.field("game_id", Long.class),
                DSL.field("price", Long.class),
                DSL.field("status", String.class))
                .values(1L, gameId, 50000L, "AVAILABLE"));

        when(seatJooqRepository.findByIdsForUpdateSkipLocked(seatIds, "AVAILABLE")).thenReturn(result);

        assertThatThrownBy(() -> transactionService.holdSeatsInTransaction(userId, gameId, seatIds))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");
    }

    @Test
    void releaseBookingById_success_releasesSeats() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.addSeat(1L, BigDecimal.valueOf(50000));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = transactionService.releaseBookingById(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingEventProducer).publishBookingCancelled(any());
        verify(bookingEventProducer).publishSeatsReleased(any());
    }

    @Test
    void releaseBookingById_alreadyCancelled_skipsRelease() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.cancel();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        Booking result = transactionService.releaseBookingById(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingEventProducer, never()).publishBookingCancelled(any());
        verify(seatJooqRepository, never()).bulkUpdateStatus(any(), any(), any());
    }
}
