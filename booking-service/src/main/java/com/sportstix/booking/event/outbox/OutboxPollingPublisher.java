package com.sportstix.booking.event.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Polls outbox_events table and publishes pending events to Kafka.
 * ShedLock ensures only one instance runs polling across all replicas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingPublisher {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 5;
    private static final int SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "outboxPolling", lockAtMostFor = "30s", lockAtLeastFor = "500ms")
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        log.debug("Polling {} outbox events", events.size());

        for (OutboxEvent event : events) {
            publishEvent(event);
        }
    }

    @Transactional
    protected void publishEvent(OutboxEvent event) {
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

    @Scheduled(cron = "0 0 4 * * *")
    @SchedulerLock(name = "outboxCleanup", lockAtMostFor = "5m", lockAtLeastFor = "1m")
    @Transactional
    public void cleanupPublishedEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        int deleted = outboxEventRepository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} published outbox events older than {}", deleted, cutoff);
        }
    }
}
