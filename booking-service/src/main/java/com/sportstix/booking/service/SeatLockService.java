package com.sportstix.booking.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.common.response.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tier 1: Redis distributed lock for seat booking.
 * Prevents concurrent booking attempts across pods.
 * Circuit breaker protects against Redis outages.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeatLockService {

    private static final String LOCK_PREFIX = "lock:seat:";
    private static final long WAIT_TIME_MS = 3000;
    private static final long LEASE_TIME_MS = 5000;

    private final RedissonClient redissonClient;

    /**
     * Acquires distributed locks for multiple seats in sorted order (deadlock prevention).
     * Returns acquired locks for cleanup.
     */
    @CircuitBreaker(name = "redisLock", fallbackMethod = "acquireLocksFallback")
    public List<RLock> acquireLocks(Collection<Long> gameSeatIds) {
        List<Long> sortedIds = new ArrayList<>(gameSeatIds);
        Collections.sort(sortedIds);

        List<RLock> acquiredLocks = new ArrayList<>();
        try {
            for (Long seatId : sortedIds) {
                RLock lock = redissonClient.getLock(LOCK_PREFIX + seatId);
                boolean acquired = lock.tryLock(WAIT_TIME_MS, LEASE_TIME_MS, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    releaseLocks(acquiredLocks);
                    throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED,
                            "Failed to acquire lock for seat: " + seatId);
                }
                acquiredLocks.add(lock);
            }
            return acquiredLocks;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            releaseLocks(acquiredLocks);
            throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED,
                    "Lock acquisition interrupted");
        }
    }

    @SuppressWarnings("unused")
    private List<RLock> acquireLocksFallback(Collection<Long> gameSeatIds, Throwable t) {
        log.error("Redis circuit breaker open. Seat lock unavailable for seats: {}", gameSeatIds, t);
        throw new BusinessException(ErrorCode.LOCK_ACQUISITION_FAILED,
                "Service temporarily unavailable. Please try again shortly.");
    }

    public void releaseLocks(List<RLock> locks) {
        for (RLock lock : locks) {
            try {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            } catch (Exception e) {
                log.warn("Failed to release lock: {}", lock.getName(), e);
            }
        }
    }
}
