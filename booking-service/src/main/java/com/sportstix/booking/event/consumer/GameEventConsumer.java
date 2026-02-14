package com.sportstix.booking.event.consumer;

import com.sportstix.booking.domain.LocalGame;
import com.sportstix.booking.domain.LocalGameSeat;
import com.sportstix.booking.event.IdempotencyService;
import com.sportstix.booking.repository.LocalGameRepository;
import com.sportstix.booking.repository.LocalGameSeatRepository;
import com.sportstix.common.event.GameInfoUpdatedEvent;
import com.sportstix.common.event.SeatInitializedEvent;
import com.sportstix.common.event.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes game events to sync local replica tables in booking_db.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventConsumer {

    private final LocalGameRepository localGameRepository;
    private final LocalGameSeatRepository localGameSeatRepository;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = Topics.GAME_SEAT_INITIALIZED, groupId = "booking-service")
    @Transactional
    public void handleSeatInitialized(SeatInitializedEvent event) {
        if (idempotencyService.isDuplicate(event.getEventId(), Topics.GAME_SEAT_INITIALIZED)) {
            log.debug("Duplicate seat-initialized event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("Received seat-initialized event: gameId={}, seats={}",
                event.getGameId(), event.getSeats() != null ? event.getSeats().size() : 0);

        // Upsert local game
        LocalGame localGame = localGameRepository.findById(event.getGameId())
                .map(existing -> {
                    existing.updateFrom(event.getHomeTeam(), event.getAwayTeam(),
                            event.getGameDate(), event.getTicketOpenAt(),
                            event.getStatus(), event.getMaxTicketsPerUser());
                    return existing;
                })
                .orElseGet(() -> new LocalGame(
                        event.getGameId(), event.getHomeTeam(), event.getAwayTeam(),
                        event.getGameDate(), event.getTicketOpenAt(),
                        event.getStatus(), event.getMaxTicketsPerUser()
                ));
        localGameRepository.save(localGame);

        // Delete existing seats before re-initialization (idempotency)
        localGameSeatRepository.deleteByGameId(event.getGameId());

        // Batch insert seats
        if (event.getSeats() != null && !event.getSeats().isEmpty()) {
            List<LocalGameSeat> seats = new ArrayList<>();
            for (SeatInitializedEvent.SeatInfo seatInfo : event.getSeats()) {
                seats.add(new LocalGameSeat(
                        seatInfo.getGameSeatId(),
                        event.getGameId(),
                        seatInfo.getSeatId(),
                        seatInfo.getSectionId(),
                        seatInfo.getPrice(),
                        seatInfo.getRowName(),
                        seatInfo.getSeatNumber()
                ));
            }
            localGameSeatRepository.saveAll(seats);
            log.info("Synced {} seats for gameId={}", seats.size(), event.getGameId());
        }

        idempotencyService.markProcessed(event.getEventId(), Topics.GAME_SEAT_INITIALIZED);
    }

    @KafkaListener(topics = Topics.GAME_INFO_UPDATED, groupId = "booking-service")
    @Transactional
    public void handleGameInfoUpdated(GameInfoUpdatedEvent event) {
        if (idempotencyService.isDuplicate(event.getEventId(), Topics.GAME_INFO_UPDATED)) {
            log.debug("Duplicate game-info-updated event skipped: eventId={}", event.getEventId());
            return;
        }

        log.info("Received game-info-updated event: gameId={}", event.getGameId());

        localGameRepository.findById(event.getGameId())
                .ifPresentOrElse(
                        existing -> {
                            existing.updateFrom(event.getHomeTeam(), event.getAwayTeam(),
                                    event.getGameDate(), event.getTicketOpenAt(),
                                    event.getStatus(), event.getMaxTicketsPerUser());
                            localGameRepository.save(existing);
                            log.info("Updated local game replica: gameId={}", event.getGameId());
                        },
                        () -> {
                            LocalGame localGame = new LocalGame(
                                    event.getGameId(), event.getHomeTeam(), event.getAwayTeam(),
                                    event.getGameDate(), event.getTicketOpenAt(),
                                    event.getStatus(), event.getMaxTicketsPerUser()
                            );
                            localGameRepository.save(localGame);
                            log.info("Created local game replica: gameId={}", event.getGameId());
                        }
                );

        idempotencyService.markProcessed(event.getEventId(), Topics.GAME_INFO_UPDATED);
    }
}
