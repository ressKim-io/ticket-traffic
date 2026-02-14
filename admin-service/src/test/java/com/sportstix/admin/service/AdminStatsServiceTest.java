package com.sportstix.admin.service;

import com.sportstix.admin.domain.BookingStats;
import com.sportstix.admin.domain.ProcessedEvent;
import com.sportstix.admin.dto.response.DashboardResponse;
import com.sportstix.admin.repository.BookingStatsRepository;
import com.sportstix.admin.repository.ProcessedEventRepository;
import com.sportstix.common.event.BookingEvent;
import com.sportstix.common.event.GameInfoUpdatedEvent;
import com.sportstix.common.event.PaymentEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceTest {

    @Mock
    private BookingStatsRepository bookingStatsRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private AdminStatsService statsService;

    private BookingStats stats;

    @BeforeEach
    void setUp() {
        stats = new BookingStats(1L);
    }

    @Test
    @DisplayName("processBookingCreated - increments total bookings and adds revenue")
    void processBookingCreated_updatesStats() {
        BookingEvent event = BookingEvent.created(10L, 1L, 1L, 100L, new BigDecimal("50000"));
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(bookingStatsRepository.findByGameId(1L)).thenReturn(Optional.of(stats));

        statsService.processBookingCreated(event);

        assertThat(stats.getTotalBookings()).isEqualTo(1);
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("50000");
        verify(bookingStatsRepository).save(stats);
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("processBookingCreated - skips duplicate event")
    void processBookingCreated_duplicate_skips() {
        BookingEvent event = BookingEvent.created(10L, 1L, 1L, 100L, new BigDecimal("50000"));
        when(processedEventRepository.existsById(anyString())).thenReturn(true);

        statsService.processBookingCreated(event);

        verify(bookingStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("processBookingConfirmed - increments confirmed count")
    void processBookingConfirmed_updatesStats() {
        BookingEvent event = BookingEvent.confirmed(10L, 1L, 1L, 100L);
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(bookingStatsRepository.findByGameId(1L)).thenReturn(Optional.of(stats));

        statsService.processBookingConfirmed(event);

        assertThat(stats.getConfirmedBookings()).isEqualTo(1);
        verify(bookingStatsRepository).save(stats);
    }

    @Test
    @DisplayName("processBookingCancelled - increments cancelled count")
    void processBookingCancelled_updatesStats() {
        BookingEvent event = BookingEvent.cancelled(10L, 1L, 1L, 100L);
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(bookingStatsRepository.findByGameId(1L)).thenReturn(Optional.of(stats));

        statsService.processBookingCancelled(event);

        assertThat(stats.getCancelledBookings()).isEqualTo(1);
        verify(bookingStatsRepository).save(stats);
    }

    @Test
    @DisplayName("processPaymentCompleted - marks event processed")
    void processPaymentCompleted_marksProcessed() {
        PaymentEvent event = PaymentEvent.completed(1L, 10L, 1L, new BigDecimal("50000"));
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        statsService.processPaymentCompleted(event);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
        verify(bookingStatsRepository, never()).save(any());
    }

    @Test
    @DisplayName("processPaymentRefunded - marks event processed")
    void processPaymentRefunded_marksProcessed() {
        PaymentEvent event = PaymentEvent.refunded(1L, 10L, 1L, new BigDecimal("50000"));
        when(processedEventRepository.existsById(anyString())).thenReturn(false);

        statsService.processPaymentRefunded(event);

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    @DisplayName("processGameInfoUpdated - updates home/away team info")
    void processGameInfoUpdated_updatesGameInfo() {
        GameInfoUpdatedEvent event = new GameInfoUpdatedEvent(
                1L, "KIA Tigers", "Samsung Lions",
                LocalDateTime.now(), LocalDateTime.now(), "OPEN", 4);
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(bookingStatsRepository.findByGameId(1L)).thenReturn(Optional.of(stats));

        statsService.processGameInfoUpdated(event);

        assertThat(stats.getHomeTeam()).isEqualTo("KIA Tigers");
        assertThat(stats.getAwayTeam()).isEqualTo("Samsung Lions");
        verify(bookingStatsRepository).save(stats);
    }

    @Test
    @DisplayName("getDashboard - returns aggregated dashboard response")
    void getDashboard_returnsAggregates() {
        DashboardResponse expected = new DashboardResponse(100, 80, new BigDecimal("5000000"), 5L);
        when(bookingStatsRepository.getDashboardAggregates()).thenReturn(expected);

        DashboardResponse result = statsService.getDashboard();

        assertThat(result.totalBookings()).isEqualTo(100);
        assertThat(result.confirmedBookings()).isEqualTo(80);
        assertThat(result.totalRevenue()).isEqualByComparingTo("5000000");
        assertThat(result.totalGames()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getGameStats - returns per-game statistics")
    void getGameStats_returnsGameStats() {
        stats.incrementTotalBookings();
        stats.incrementConfirmed();
        stats.addRevenue(new BigDecimal("50000"));
        when(bookingStatsRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(stats));

        var result = statsService.getGameStats();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).gameId()).isEqualTo(1L);
        assertThat(result.get(0).totalBookings()).isEqualTo(1);
    }

    @Test
    @DisplayName("getOrCreateStats - creates new stats when not found")
    void processBookingCreated_newGame_createsStats() {
        BookingEvent event = BookingEvent.created(10L, 1L, 99L, 100L, new BigDecimal("30000"));
        when(processedEventRepository.existsById(anyString())).thenReturn(false);
        when(bookingStatsRepository.findByGameId(99L)).thenReturn(Optional.empty());
        when(bookingStatsRepository.saveAndFlush(any(BookingStats.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        statsService.processBookingCreated(event);

        verify(bookingStatsRepository).saveAndFlush(any(BookingStats.class));
        verify(bookingStatsRepository).save(any(BookingStats.class));
    }
}
