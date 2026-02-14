package com.sportstix.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReconciliationSchedulerTest {

    @Mock
    private DataReconciliationService reconciliationService;
    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private ReconciliationScheduler scheduler;

    @Test
    void runReconciliation_lockAcquired_executesAllChecks() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("lock:reconciliation")).thenReturn(lock);
        when(lock.tryLock(0, 240, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(reconciliationService.reconcileStaleHeldSeats()).thenReturn(2);
        when(reconciliationService.detectBookingSeatMismatch()).thenReturn(0);
        when(reconciliationService.reconcileExpiredPendingBookings()).thenReturn(1);

        scheduler.runReconciliation();

        verify(reconciliationService).reconcileStaleHeldSeats();
        verify(reconciliationService).detectBookingSeatMismatch();
        verify(reconciliationService).reconcileExpiredPendingBookings();
        verify(lock).unlock();
    }

    @Test
    void runReconciliation_lockNotAcquired_skipsExecution() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("lock:reconciliation")).thenReturn(lock);
        when(lock.tryLock(0, 240, TimeUnit.SECONDS)).thenReturn(false);

        scheduler.runReconciliation();

        verify(reconciliationService, never()).reconcileStaleHeldSeats();
        verify(reconciliationService, never()).detectBookingSeatMismatch();
        verify(reconciliationService, never()).reconcileExpiredPendingBookings();
    }

    @Test
    void runReconciliation_exceptionDuringExecution_unlocksAndContinues() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("lock:reconciliation")).thenReturn(lock);
        when(lock.tryLock(0, 240, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        when(reconciliationService.reconcileStaleHeldSeats()).thenThrow(new RuntimeException("DB error"));

        scheduler.runReconciliation();

        verify(lock).unlock();
    }

    @Test
    void runReconciliation_interrupted_restoresInterruptFlag() throws InterruptedException {
        RLock lock = mock(RLock.class);
        when(redissonClient.getLock("lock:reconciliation")).thenReturn(lock);
        when(lock.tryLock(0, 240, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

        scheduler.runReconciliation();

        verify(reconciliationService, never()).reconcileStaleHeldSeats();
    }
}
