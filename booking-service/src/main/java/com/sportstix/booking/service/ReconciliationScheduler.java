package com.sportstix.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Runs data reconciliation checks on a fixed schedule.
 * Uses Redisson distributed lock to prevent duplicate execution across pods.
 * Calls each reconciliation method individually (not via runAll) to ensure
 * Spring @Transactional proxy is properly invoked.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private static final String LOCK_KEY = "lock:reconciliation";
    private static final long LOCK_LEASE_SECONDS = 240; // 4 min max hold

    private final DataReconciliationService reconciliationService;
    private final RedissonClient redissonClient;

    @Scheduled(fixedRate = 300_000, initialDelay = 60_000) // Every 5 minutes, 1 min initial delay
    public void runReconciliation() {
        RLock lock = redissonClient.getLock(LOCK_KEY);

        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (!acquired) {
            log.debug("RECONCILE: Another pod is running reconciliation, skipping");
            return;
        }

        try {
            log.info("RECONCILE: Starting scheduled data reconciliation");

            // Call each method via proxy to preserve @Transactional
            int staleSeats = reconciliationService.reconcileStaleHeldSeats();
            int seatMismatches = reconciliationService.detectBookingSeatMismatch();
            int expiredBookings = reconciliationService.reconcileExpiredPendingBookings();

            Map<String, Integer> results = Map.of(
                    "staleHeldSeatsReleased", staleSeats,
                    "bookingSeatMismatches", seatMismatches,
                    "expiredPendingBookings", expiredBookings
            );
            log.info("RECONCILE: Completed - results={}", results);
        } catch (Exception e) {
            log.error("RECONCILE: Failed to complete reconciliation", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
