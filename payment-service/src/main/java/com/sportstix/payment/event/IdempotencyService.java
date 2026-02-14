package com.sportstix.payment.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Idempotency service for Kafka consumer event deduplication.
 * Uses DB unique constraint on event_id to prevent duplicate processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ProcessedEventRepository processedEventRepository;

    public boolean isDuplicate(String eventId, String topic) {
        if (eventId == null) {
            return false;
        }
        return processedEventRepository.existsById(eventId);
    }

    public void markProcessed(String eventId, String topic) {
        if (eventId == null) {
            return;
        }
        try {
            processedEventRepository.save(new ProcessedEvent(eventId, topic));
        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate event insert ignored: eventId={}", eventId);
        }
    }
}
