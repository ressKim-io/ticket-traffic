package com.sportstix.booking.service;

import com.sportstix.booking.domain.Booking;
import com.sportstix.booking.domain.BookingStatus;
import com.sportstix.booking.domain.LocalGame;
import com.sportstix.booking.event.producer.BookingEventProducer;
import com.sportstix.booking.jooq.LocalGameSeatJooqRepository;
import com.sportstix.booking.repository.BookingRepository;
import com.sportstix.booking.repository.LocalGameRepository;
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
import org.redisson.api.RLock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private LocalGameRepository localGameRepository;
    @Mock
    private LocalGameSeatJooqRepository seatJooqRepository;
    @Mock
    private SeatLockService seatLockService;
    @Mock
    private BookingEventProducer bookingEventProducer;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void holdSeats_success_createsBookingAndHoldsSeats() {
        Long userId = 100L;
        Long gameId = 10L;
        Set<Long> seatIds = Set.of(1L, 2L);

        LocalGame game = createGame(gameId, 4);
        when(localGameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(bookingRepository.countByUserIdAndGameIdAndStatusIn(eq(userId), eq(gameId), any()))
                .thenReturn(0L);
        when(seatLockService.acquireLocks(seatIds)).thenReturn(List.of(mock(RLock.class)));

        // Mock jOOQ result
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

        Booking booking = bookingService.holdSeats(userId, gameId, seatIds);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getTotalPrice()).isEqualTo(BigDecimal.valueOf(100000));
        assertThat(booking.getBookingSeats()).hasSize(2);
        verify(bookingEventProducer).publishBookingCreated(any());
        verify(bookingEventProducer).publishSeatsHeld(any());
        verify(seatLockService).releaseLocks(any());
    }

    @Test
    void holdSeats_exceedsMaxTickets_throwsException() {
        Long userId = 100L;
        Long gameId = 10L;

        LocalGame game = createGame(gameId, 4);
        when(localGameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(bookingRepository.countByUserIdAndGameIdAndStatusIn(eq(userId), eq(gameId), any()))
                .thenReturn(3L);

        assertThatThrownBy(() -> bookingService.holdSeats(userId, gameId, Set.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Exceeds max tickets");
    }

    @Test
    void holdSeats_seatsNotAvailable_throwsException() {
        Long userId = 100L;
        Long gameId = 10L;
        Set<Long> seatIds = Set.of(1L, 2L);

        LocalGame game = createGame(gameId, 4);
        when(localGameRepository.findById(gameId)).thenReturn(Optional.of(game));
        when(bookingRepository.countByUserIdAndGameIdAndStatusIn(eq(userId), eq(gameId), any()))
                .thenReturn(0L);
        when(seatLockService.acquireLocks(seatIds)).thenReturn(List.of(mock(RLock.class)));

        // Return only 1 seat (seat 2 is already taken)
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

        assertThatThrownBy(() -> bookingService.holdSeats(userId, gameId, seatIds))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no longer available");

        verify(seatLockService).releaseLocks(any());
    }

    @Test
    void confirmBooking_success_confirmsAndReservesSeats() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.addSeat(1L, BigDecimal.valueOf(50000));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(seatJooqRepository.bulkUpdateStatus(any(), eq("HELD"), eq("RESERVED"))).thenReturn(1);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.confirmBooking(1L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingEventProducer).publishBookingConfirmed(any());
    }

    @Test
    void cancelBooking_success_releasesSeats() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        booking.addSeat(1L, BigDecimal.valueOf(50000));

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking result = bookingService.cancelBooking(1L, 100L);

        assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingEventProducer).publishBookingCancelled(any());
        verify(bookingEventProducer).publishSeatsReleased(any());
    }

    @Test
    void cancelBooking_wrongUser_throwsForbidden() {
        Booking booking = Booking.builder().userId(100L).gameId(10L).build();
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancelBooking(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User does not own booking");
    }

    private LocalGame createGame(Long gameId, int maxTickets) {
        return new LocalGame(gameId, "Home", "Away",
                LocalDateTime.of(2025, 6, 15, 19, 0),
                LocalDateTime.of(2025, 6, 10, 10, 0),
                "OPEN", maxTickets);
    }
}
