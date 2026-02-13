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
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
        given(zSetOperations.score("queue:1", "100")).willReturn(null);
        given(tokenService.hasToken(1L, 100L)).willReturn(false);
        given(zSetOperations.add(eq("queue:1"), eq("100"), anyDouble())).willReturn(true);
        given(zSetOperations.rank("queue:1", "100")).willReturn(0L);
        given(zSetOperations.size("queue:1")).willReturn(1L);

        QueueStatusResponse result = queueService.enterQueue(1L, 100L);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.rank()).isEqualTo(1);
        verify(queueEventProducer).publishEntered(1L, 100L);
    }

    @Test
    void enterQueue_existingUser_returnsCurrentStatus() {
        given(zSetOperations.score("queue:1", "100")).willReturn(1000.0);
        // getQueueStatus path
        given(tokenService.hasToken(1L, 100L)).willReturn(false);
        given(zSetOperations.rank("queue:1", "100")).willReturn(5L);
        given(zSetOperations.size("queue:1")).willReturn(50L);

        QueueStatusResponse result = queueService.enterQueue(1L, 100L);

        assertThat(result.status()).isEqualTo("WAITING");
        assertThat(result.rank()).isEqualTo(6);
    }

    @Test
    void enterQueue_hasToken_returnsEligible() {
        given(zSetOperations.score("queue:1", "100")).willReturn(null);
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
    void leaveQueue_removesFromQueueAndToken() {
        queueService.leaveQueue(1L, 100L);

        verify(zSetOperations).remove("queue:1", "100");
        verify(tokenService).revokeToken(1L, 100L);
    }

    @Test
    void popNextBatch_returnsBatchAndRemoves() {
        Set<String> batch = new LinkedHashSet<>();
        batch.add("1");
        batch.add("2");
        batch.add("3");
        given(zSetOperations.range("queue:1", 0, 99)).willReturn(batch);

        Set<String> result = queueService.popNextBatch(1L);

        assertThat(result).hasSize(3);
        verify(zSetOperations).remove(eq("queue:1"), eq("1"), eq("2"), eq("3"));
    }

    @Test
    void getQueueSize_returnsSize() {
        given(zSetOperations.size("queue:1")).willReturn(500L);

        Long size = queueService.getQueueSize(1L);

        assertThat(size).isEqualTo(500);
    }

    @Test
    void addToActive_addsToSet() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        queueService.addToActive(1L, 100L);

        verify(setOperations).add("queue:active:1", "100");
    }
}
