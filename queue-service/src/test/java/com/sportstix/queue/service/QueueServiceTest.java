package com.sportstix.queue.service;

import com.sportstix.common.exception.BusinessException;
import com.sportstix.queue.config.QueueProperties;
import com.sportstix.queue.dto.response.QueueStatusResponse;
import com.sportstix.queue.event.producer.QueueEventProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    private QueueService queueService;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private TokenService tokenService;
    @Mock
    private QueueEventProducer queueEventProducer;

    private QueueProperties queueProperties;

    @BeforeEach
    void setUp() {
        queueProperties = new QueueProperties();
        queueProperties.setBatchSize(100);
        queueProperties.setTokenTtlSeconds(600);
        queueProperties.setProcessIntervalMs(3000);

        org.mockito.Mockito.lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        queueService = new QueueService(redisTemplate, tokenService, queueEventProducer, queueProperties);
    }

    @Test
    void enterQueue_newUser_addsToSortedSet() {
        given(tokenService.hasToken(1L, 100L)).willReturn(false);
        given(zSetOperations.addIfAbsent(eq("queue:1"), eq("100"), anyDouble())).willReturn(true);
        given(zSetOperations.rank("queue:1", "100")).willReturn(0L);
        given(zSetOperations.size("queue:1")).willReturn(1L);

        QueueStatusResponse result = queueService.enterQueue(1L, 100L);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.rank()).isEqualTo(1);
        verify(queueEventProducer).publishEntered(1L, 100L);
    }

    @Test
    void enterQueue_existingUser_returnsCurrentStatus() {
        given(tokenService.hasToken(1L, 100L)).willReturn(false);
        given(zSetOperations.addIfAbsent(eq("queue:1"), eq("100"), anyDouble())).willReturn(false);
        // getQueueStatus path
        given(zSetOperations.rank("queue:1", "100")).willReturn(5L);
        given(zSetOperations.size("queue:1")).willReturn(50L);

        QueueStatusResponse result = queueService.enterQueue(1L, 100L);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.rank()).isEqualTo(6);
    }

    @Test
    void enterQueue_hasToken_returnsEligible() {
        given(tokenService.hasToken(1L, 100L)).willReturn(true);
        given(tokenService.getToken(1L, 100L)).willReturn("test-token");

        QueueStatusResponse result = queueService.enterQueue(1L, 100L);

        assertThat(result.status()).isEqualTo("ELIGIBLE");
        assertThat(result.token()).isEqualTo("test-token");
    }

    @Test
    void getQueueStatus_notInQueue_throwsException() {
        given(tokenService.hasToken(1L, 100L)).willReturn(false);
        given(zSetOperations.rank("queue:1", "100")).willReturn(null);

        assertThatThrownBy(() -> queueService.getQueueStatus(1L, 100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void leaveQueue_removesFromQueueAndTokenAndActive() {
        queueService.leaveQueue(1L, 100L);

        verify(zSetOperations).remove("queue:1", "100");
        verify(setOperations).remove("queue:active:1", "100");
        verify(tokenService).revokeToken(1L, 100L);
    }

    @Test
    void popNextBatch_returnsBatchAtomically() {
        Set<ZSetOperations.TypedTuple<String>> tuples = new LinkedHashSet<>();
        tuples.add(new DefaultTypedTuple<>("1", 0.0));
        tuples.add(new DefaultTypedTuple<>("2", 1.0));
        tuples.add(new DefaultTypedTuple<>("3", 2.0));
        given(zSetOperations.popMin("queue:1", 100)).willReturn(tuples);

        Set<String> result = queueService.popNextBatch(1L);

        assertThat(result).containsExactly("1", "2", "3");
    }

    @Test
    void popNextBatch_emptyQueue_returnsEmptySet() {
        given(zSetOperations.popMin("queue:1", 100)).willReturn(Set.of());

        Set<String> result = queueService.popNextBatch(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getQueueSize_returnsSize() {
        given(zSetOperations.size("queue:1")).willReturn(500L);

        Long size = queueService.getQueueSize(1L);

        assertThat(size).isEqualTo(500);
    }

    @Test
    void addToActive_addsToSet() {
        queueService.addToActive(1L, 100L);

        verify(setOperations).add("queue:active:1", "100");
    }
}
