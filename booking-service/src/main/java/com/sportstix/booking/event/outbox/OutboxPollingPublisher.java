package com.sportstix.booking.event.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Polls outbox_events table and publishes pending events to Kafka.
 * ShedLock ensures only one instance runs polling across all replicas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPollingPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Scheduled(fixedDelay = 1000)
    @SchedulerLock(name = "outboxPolling", lockAtMostFor = "30s", lockAtLeastFor = "500ms")
    public void pollAndPublish() {
        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(BATCH_SIZE);
        if (events.isEmpty()) {
            return;
        }

        log.debug("Polling {} outbox events", events.size());

        for (OutboxEvent event : events) {
            outboxEventPublisher.publishEvent(event);
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
