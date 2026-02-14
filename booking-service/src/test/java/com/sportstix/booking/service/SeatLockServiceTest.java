package com.sportstix.booking.service;

import com.sportstix.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private SeatLockService seatLockService;

    @Test
    void acquireLocks_success_returnsLocks() throws InterruptedException {
        RLock lock1 = mock(RLock.class);
        RLock lock2 = mock(RLock.class);

        when(redissonClient.getLock("lock:seat:1")).thenReturn(lock1);
        when(redissonClient.getLock("lock:seat:2")).thenReturn(lock2);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock2.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        List<RLock> locks = seatLockService.acquireLocks(Set.of(1L, 2L));

        assertThat(locks).hasSize(2);
    }

    @Test
    void acquireLocks_partialFailure_releasesAcquiredAndThrows() throws InterruptedException {
        RLock lock1 = mock(RLock.class);
        RLock lock2 = mock(RLock.class);

        when(redissonClient.getLock("lock:seat:1")).thenReturn(lock1);
        when(redissonClient.getLock("lock:seat:2")).thenReturn(lock2);
        when(lock1.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock2.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(lock1.isHeldByCurrentThread()).thenReturn(true);

        assertThatThrownBy(() -> seatLockService.acquireLocks(Set.of(1L, 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to acquire lock");

        verify(lock1).unlock();
    }

    @Test
    void releaseLocks_releasesAllHeldLocks() {
        RLock lock1 = mock(RLock.class);
        RLock lock2 = mock(RLock.class);

        when(lock1.isHeldByCurrentThread()).thenReturn(true);
        when(lock2.isHeldByCurrentThread()).thenReturn(false);

        seatLockService.releaseLocks(List.of(lock1, lock2));

        verify(lock1).unlock();
        verify(lock2, never()).unlock();
    }
}
