package com.sportstix.admin.service;

import com.sportstix.admin.domain.BookingStats;
import com.sportstix.admin.domain.ProcessedEvent;
import com.sportstix.admin.dto.response.DashboardResponse;
import com.sportstix.admin.dto.response.GameStatsResponse;
import com.sportstix.admin.repository.BookingStatsRepository;
import com.sportstix.admin.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatsService {

    private final BookingStatsRepository bookingStatsRepository;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Check if event already processed (idempotency).
     */
    public boolean isProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    /**
     * Mark event as processed.
     */
    @Transactional
    public void markProcessed(String eventId, String topic) {
        processedEventRepository.save(new ProcessedEvent(eventId, topic));
    }

    @Transactional
    public void onBookingCreated(Long gameId, BigDecimal totalPrice) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.incrementTotalBookings();
        bookingStatsRepository.save(stats);
        log.debug("Booking created stats updated: gameId={}", gameId);
    }

    @Transactional
    public void onBookingConfirmed(Long gameId) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.incrementConfirmed();
        bookingStatsRepository.save(stats);
        log.debug("Booking confirmed stats updated: gameId={}", gameId);
    }

    @Transactional
    public void onBookingCancelled(Long gameId) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.incrementCancelled();
        bookingStatsRepository.save(stats);
        log.debug("Booking cancelled stats updated: gameId={}", gameId);
    }

    @Transactional
    public void onPaymentCompleted(Long gameId, BigDecimal amount) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.addRevenue(amount);
        bookingStatsRepository.save(stats);
        log.debug("Payment completed stats updated: gameId={}", gameId);
    }

    @Transactional
    public void onPaymentRefunded(Long gameId, BigDecimal amount) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.addRefund(amount);
        bookingStatsRepository.save(stats);
        log.debug("Payment refunded stats updated: gameId={}", gameId);
    }

    @Transactional
    public void onGameInfoUpdated(Long gameId, String homeTeam, String awayTeam) {
        BookingStats stats = getOrCreateStats(gameId);
        stats.updateGameInfo(homeTeam, awayTeam);
        bookingStatsRepository.save(stats);
        log.debug("Game info updated: gameId={}", gameId);
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        int totalBookings = bookingStatsRepository.sumTotalBookings();
        int confirmedBookings = bookingStatsRepository.sumConfirmedBookings();
        BigDecimal totalRevenue = bookingStatsRepository.sumTotalRevenue();
        long totalGames = bookingStatsRepository.count();

        return new DashboardResponse(
                totalBookings,
                confirmedBookings,
                totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                totalGames
        );
    }

    @Transactional(readOnly = true)
    public List<GameStatsResponse> getGameStats() {
        return bookingStatsRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(GameStatsResponse::from)
                .toList();
    }

    private BookingStats getOrCreateStats(Long gameId) {
        return bookingStatsRepository.findByGameId(gameId)
                .orElseGet(() -> bookingStatsRepository.save(new BookingStats(gameId)));
    }
}
