package com.sportstix.admin.service;

import com.sportstix.admin.domain.BookingStats;
import com.sportstix.admin.domain.ProcessedEvent;
import com.sportstix.admin.dto.response.DashboardResponse;
import com.sportstix.admin.dto.response.GameStatsResponse;
import com.sportstix.admin.repository.BookingStatsRepository;
import com.sportstix.admin.repository.ProcessedEventRepository;
import com.sportstix.common.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

    // ── Event handlers (single transaction per event) ──

    @Transactional
    public void processBookingCreated(BookingEvent event) {
        if (isDuplicate(event.getEventId())) return;
        BookingStats stats = getOrCreateStats(event.getGameId());
        stats.incrementTotalBookings();
        stats.addRevenue(event.getTotalPrice());
        bookingStatsRepository.save(stats);
        markProcessed(event.getEventId(), Topics.BOOKING_CREATED);
        log.info("Booking created stats updated: gameId={}", event.getGameId());
    }

    @Transactional
    public void processBookingConfirmed(BookingEvent event) {
        if (isDuplicate(event.getEventId())) return;
        BookingStats stats = getOrCreateStats(event.getGameId());
        stats.incrementConfirmed();
        bookingStatsRepository.save(stats);
        markProcessed(event.getEventId(), Topics.BOOKING_CONFIRMED);
        log.info("Booking confirmed stats updated: gameId={}", event.getGameId());
    }

    @Transactional
    public void processBookingCancelled(BookingEvent event) {
        if (isDuplicate(event.getEventId())) return;
        BookingStats stats = getOrCreateStats(event.getGameId());
        stats.incrementCancelled();
        bookingStatsRepository.save(stats);
        markProcessed(event.getEventId(), Topics.BOOKING_CANCELLED);
        log.info("Booking cancelled stats updated: gameId={}", event.getGameId());
    }

    @Transactional
    public void processPaymentCompleted(PaymentEvent event) {
        if (isDuplicate(event.getEventId())) return;
        markProcessed(event.getEventId(), Topics.PAYMENT_COMPLETED);
        log.info("Payment completed processed: paymentId={}", event.getPaymentId());
    }

    @Transactional
    public void processPaymentRefunded(PaymentEvent event) {
        if (isDuplicate(event.getEventId())) return;
        markProcessed(event.getEventId(), Topics.PAYMENT_REFUNDED);
        log.info("Payment refunded processed: paymentId={}", event.getPaymentId());
    }

    @Transactional
    public void processGameInfoUpdated(GameInfoUpdatedEvent event) {
        if (isDuplicate(event.getEventId())) return;
        BookingStats stats = getOrCreateStats(event.getGameId());
        stats.updateGameInfo(event.getHomeTeam(), event.getAwayTeam());
        bookingStatsRepository.save(stats);
        markProcessed(event.getEventId(), Topics.GAME_INFO_UPDATED);
        log.info("Game info updated: gameId={}", event.getGameId());
    }

    // ── Query methods ──

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        return bookingStatsRepository.getDashboardAggregates();
    }

    @Transactional(readOnly = true)
    public List<GameStatsResponse> getGameStats() {
        return bookingStatsRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(GameStatsResponse::from)
                .toList();
    }

    // ── Helpers ──

    private boolean isDuplicate(String eventId) {
        if (processedEventRepository.existsById(eventId)) {
            log.debug("Event already processed: {}", eventId);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventId, String topic) {
        processedEventRepository.save(new ProcessedEvent(eventId, topic));
    }

    private BookingStats getOrCreateStats(Long gameId) {
        return bookingStatsRepository.findByGameId(gameId)
                .orElseGet(() -> {
                    try {
                        return bookingStatsRepository.saveAndFlush(new BookingStats(gameId));
                    } catch (DataIntegrityViolationException e) {
                        return bookingStatsRepository.findByGameId(gameId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "BookingStats for gameId=" + gameId + " disappeared"));
                    }
                });
    }
}
