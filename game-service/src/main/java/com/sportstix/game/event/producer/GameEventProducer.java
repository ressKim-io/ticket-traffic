package com.sportstix.game.event.producer;

import com.sportstix.common.event.GameEvent;
import com.sportstix.common.event.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSeatInitialized(Long gameId, int totalSeats) {
        GameEvent event = GameEvent.seatInitialized(gameId, totalSeats);
        kafkaTemplate.send(Topics.GAME_SEAT_INITIALIZED, String.valueOf(gameId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish seat-initialized event for gameId={}: {}", gameId, ex.getMessage());
                    } else {
                        log.info("Published seat-initialized event for gameId={}, totalSeats={}", gameId, totalSeats);
                    }
                });
    }

    public void publishInfoUpdated(Long gameId) {
        GameEvent event = GameEvent.infoUpdated(gameId);
        kafkaTemplate.send(Topics.GAME_INFO_UPDATED, String.valueOf(gameId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish info-updated event for gameId={}: {}", gameId, ex.getMessage());
                    } else {
                        log.info("Published info-updated event for gameId={}", gameId);
                    }
                });
    }
}
