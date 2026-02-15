package com.sportstix.booking.event.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Saves events to the outbox table within the caller's transaction.
 * Must be called inside an existing @Transactional context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void save(String aggregateType, String aggregateId,
                     String eventType, String topic,
                     String partitionKey, Object event) {
        String payload = serialize(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                aggregateType, aggregateId, eventType,
                topic, partitionKey, payload);

        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox event saved: type={}, topic={}, aggregateId={}",
                eventType, topic, aggregateId);
    }

    private String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize outbox event", e);
        }
    }
}
