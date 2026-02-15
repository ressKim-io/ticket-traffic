package com.sportstix.booking.event.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Publishes a single outbox event to Kafka within a transaction.
 * Separated from OutboxPollingPublisher to ensure @Transactional proxy works
 * (avoids self-invocation bypass).
 *
 * Guarantees at-least-once delivery: if DB commit fails after Kafka send,
 * the event will be re-published. Consumers MUST be idempotent.
 */
@Slf4j
@Service
public class OutboxEventPublisher {

    private static final int MAX_RETRIES = 5;
    private static final int SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(
            OutboxEventRepository outboxEventRepository,
            @Qualifier("outboxKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void publishEvent(OutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            event.markPublished();
            outboxEventRepository.save(event);
            log.debug("Outbox event published: id={}, topic={}", event.getId(), event.getTopic());
        } catch (Exception e) {
            handlePublishFailure(event, e);
        }
    }

    private void handlePublishFailure(OutboxEvent event, Exception e) {
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.markFailed();
            outboxEventRepository.save(event);
            log.error("Outbox event permanently failed after {} retries: id={}, topic={}",
                    MAX_RETRIES, event.getId(), event.getTopic(), e);
        } else {
            event.markRetrying();
            outboxEventRepository.save(event);
            log.warn("Outbox event publish failed (retry {}): id={}, topic={}",
                    event.getRetryCount(), event.getId(), event.getTopic(), e);
        }
    }
}
