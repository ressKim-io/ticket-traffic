package com.sportstix.booking.event;

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

    /**
     * Returns true if event was already processed (should be skipped).
     * Returns false if event is new (should be processed).
     */
    public boolean isDuplicate(String eventId, String topic) {
        if (eventId == null) {
            return false;
        }
        return processedEventRepository.existsById(eventId);
    }

    /**
     * Marks event as processed. Safe against race conditions via DB unique constraint.
     */
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
