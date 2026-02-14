package com.sportstix.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Runs data reconciliation checks on a fixed schedule.
 * Logs results and alerts on mismatches.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final DataReconciliationService reconciliationService;

    @Scheduled(fixedRate = 300_000, initialDelay = 60_000) // Every 5 minutes, 1 min initial delay
    public void runReconciliation() {
        log.info("RECONCILE: Starting scheduled data reconciliation");
        try {
            Map<String, Integer> results = reconciliationService.runAll();
            log.info("RECONCILE: Completed - results={}", results);
        } catch (Exception e) {
            log.error("RECONCILE: Failed to complete reconciliation", e);
        }
    }
}
