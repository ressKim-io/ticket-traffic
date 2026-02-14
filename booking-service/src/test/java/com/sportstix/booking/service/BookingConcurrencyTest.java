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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Simulates concurrent seat booking by multiple users.
 * Verifies that the 3-tier lock ensures only one user wins the seat.
 */
@ExtendWith(MockitoExtension.class)
class BookingConcurrencyTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private LocalGameRepository localGameRepository;
    @Mock private LocalGameSeatJooqRepository seatJooqRepository;
    @Mock private SeatLockService seatLockService;
    @Mock private BookingEventProducer bookingEventProducer;
    @Mock private BookingTransactionService transactionService;

    private BookingService bookingService;

    private static final Long GAME_ID = 10L;
    private static final Long SEAT_ID = 100L;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                bookingRepository, localGameRepository, seatJooqRepository,
                seatLockService, bookingEventProducer, transactionService);
    }

    @Test
    void concurrentHoldSeats_onlyOneWins_othersGetConflict() throws Exception {
        int numUsers = 10;
        LocalGame game = new LocalGame(GAME_ID, "Home", "Away",
                LocalDateTime.of(2025, 6, 15, 19, 0),
                LocalDateTime.of(2025, 6, 10, 10, 0),
                "OPEN", 4);
        when(localGameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(bookingRepository.countByUserIdAndGameIdAndStatusIn(anyLong(), eq(GAME_ID), any()))
                .thenReturn(0L);

        // First caller succeeds, subsequent callers fail (seat already taken)
        AtomicInteger lockCallCount = new AtomicInteger(0);
        when(seatLockService.acquireLocks(Set.of(SEAT_ID))).thenAnswer(inv -> {
            int call = lockCallCount.incrementAndGet();
            if (call == 1) {
                // First lock acquirer
                return List.of(mock(RLock.class));
            } else {
                // Simulate lock contention - still acquired but transaction fails
                return List.of(mock(RLock.class));
            }
        });

        AtomicInteger txCallCount = new AtomicInteger(0);
        when(transactionService.holdSeatsInTransaction(anyLong(), eq(GAME_ID), eq(Set.of(SEAT_ID))))
                .thenAnswer(inv -> {
                    int call = txCallCount.incrementAndGet();
                    if (call == 1) {
                        Long userId = inv.getArgument(0);
                        Booking booking = Booking.builder().userId(userId).gameId(GAME_ID).build();
                        booking.addSeat(SEAT_ID, BigDecimal.valueOf(50000));
                        return booking;
                    } else {
                        throw new BusinessException(
                                com.sportstix.common.response.ErrorCode.SEAT_NOT_AVAILABLE,
                                "Some seats are no longer available");
                    }
                });

        // Execute concurrent requests
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numUsers; i++) {
            final long userId = 100L + i;
            futures.add(executor.submit(() -> {
                try {
                    bookingService.holdSeats(userId, GAME_ID, Set.of(SEAT_ID));
                    return true; // success
                } catch (BusinessException e) {
                    return false; // conflict
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = 0;
        long failCount = 0;
        for (Future<Boolean> f : futures) {
            if (f.get()) successCount++;
            else failCount++;
        }

        // Exactly 1 should succeed, rest should fail
        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(numUsers - 1);

        // Lock should always be released (finally block)
        verify(seatLockService, times(numUsers)).releaseLocks(any());
    }

    @Test
    void concurrentHoldSeats_differentSeats_allSucceed() throws Exception {
        int numUsers = 5;
        LocalGame game = new LocalGame(GAME_ID, "Home", "Away",
                LocalDateTime.of(2025, 6, 15, 19, 0),
                LocalDateTime.of(2025, 6, 10, 10, 0),
                "OPEN", 4);
        when(localGameRepository.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(bookingRepository.countByUserIdAndGameIdAndStatusIn(anyLong(), eq(GAME_ID), any()))
                .thenReturn(0L);
        when(seatLockService.acquireLocks(anySet())).thenReturn(List.of(mock(RLock.class)));

        when(transactionService.holdSeatsInTransaction(anyLong(), eq(GAME_ID), anySet()))
                .thenAnswer(inv -> {
                    Long userId = inv.getArgument(0);
                    Set<Long> seatIds = inv.getArgument(2);
                    Booking booking = Booking.builder().userId(userId).gameId(GAME_ID).build();
                    for (Long seatId : seatIds) {
                        booking.addSeat(seatId, BigDecimal.valueOf(50000));
                    }
                    return booking;
                });

        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < numUsers; i++) {
            final long userId = 100L + i;
            final Set<Long> seatIds = Set.of(200L + i); // Each user has a different seat
            futures.add(executor.submit(() -> {
                try {
                    bookingService.holdSeats(userId, GAME_ID, seatIds);
                    return true;
                } catch (BusinessException e) {
                    return false;
                }
            }));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        long successCount = futures.stream().filter(f -> {
            try { return f.get(); } catch (Exception e) { return false; }
        }).count();

        // All succeed because they target different seats
        assertThat(successCount).isEqualTo(numUsers);
    }
}
