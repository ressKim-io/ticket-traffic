package com.sportstix.queue.event.producer;

import com.sportstix.common.event.QueueEvent;
import com.sportstix.common.event.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEntered(Long gameId, Long userId) {
        QueueEvent event = QueueEvent.entered(gameId, userId);
        kafkaTemplate.send(Topics.QUEUE_ENTERED, String.valueOf(gameId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish queue-entered event gameId={}, userId={}: {}",
                                gameId, userId, ex.getMessage());
                    }
                });
    }

    public void publishTokenIssued(Long gameId, Long userId, String token) {
        QueueEvent event = QueueEvent.tokenIssued(gameId, userId, token);
        kafkaTemplate.send(Topics.QUEUE_TOKEN_ISSUED, String.valueOf(gameId), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish token-issued event gameId={}, userId={}: {}",
                                gameId, userId, ex.getMessage());
                    }
                });
    }
}
