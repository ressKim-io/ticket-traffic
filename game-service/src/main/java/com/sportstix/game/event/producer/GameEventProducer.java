package com.sportstix.game.event.producer;

import com.sportstix.common.event.GameInfoUpdatedEvent;
import com.sportstix.common.event.SeatInitializedEvent;
import com.sportstix.common.event.Topics;
import com.sportstix.game.domain.Game;
import com.sportstix.game.domain.GameSeat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatInitialized(Game game, List<GameSeat> gameSeats) {
        List<SeatInitializedEvent.SeatInfo> seatInfos = gameSeats.stream()
                .map(gs -> new SeatInitializedEvent.SeatInfo(
                        gs.getId(),
                        gs.getSeat().getId(),
                        gs.getSeat().getSection().getId(),
                        gs.getPrice(),
                        gs.getSeat().getRowNumber(),
                        gs.getSeat().getSeatNumber()
                ))
                .toList();

        SeatInitializedEvent event = new SeatInitializedEvent(
                game.getId(), game.getHomeTeam(), game.getAwayTeam(),
                game.getGameDate(), game.getTicketOpenAt(),
                game.getStatus().name(), game.getMaxTicketsPerUser(),
                seatInfos
        );

        kafkaTemplate.send(Topics.GAME_SEAT_INITIALIZED, String.valueOf(game.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish seat-initialized event for gameId={}: {}",
                                game.getId(), ex.getMessage());
                    } else {
                        log.info("Published seat-initialized event for gameId={}, seats={}",
                                game.getId(), seatInfos.size());
                    }
                });
    }

    public void publishInfoUpdated(Game game) {
        GameInfoUpdatedEvent event = new GameInfoUpdatedEvent(
                game.getId(), game.getHomeTeam(), game.getAwayTeam(),
                game.getGameDate(), game.getTicketOpenAt(),
                game.getStatus().name(), game.getMaxTicketsPerUser()
        );

        kafkaTemplate.send(Topics.GAME_INFO_UPDATED, String.valueOf(game.getId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish info-updated event for gameId={}: {}",
                                game.getId(), ex.getMessage());
                    } else {
                        log.info("Published info-updated event for gameId={}", game.getId());
                    }
                });
    }
}
