package com.sportstix.queue.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WaitingRoomServiceTest {

    private WaitingRoomService waitingRoomService;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @BeforeEach
    void setUp() {
        waitingRoomService = new WaitingRoomService(redisTemplate);
    }

    @Test
    void register_newUser_returnsTrue() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.add("waitingroom:1", "100")).willReturn(1L);

        boolean result = waitingRoomService.register(1L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void register_existingUser_returnsFalse() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.add("waitingroom:1", "100")).willReturn(0L);

        boolean result = waitingRoomService.register(1L, 100L);

        assertThat(result).isFalse();
    }

    @Test
    void isRegistered_memberExists_returnsTrue() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember("waitingroom:1", "100")).willReturn(true);

        boolean result = waitingRoomService.isRegistered(1L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void getWaitingCount_returnsSize() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.size("waitingroom:1")).willReturn(500L);

        Long count = waitingRoomService.getWaitingCount(1L);

        assertThat(count).isEqualTo(500);
    }

    @SuppressWarnings("unchecked")
    @Test
    void convertToQueue_shufflesAndBatchInsertsToSortedSet() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(redisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(setOperations.members("waitingroom:1")).willReturn(Set.of("1", "2", "3"));

        int count = waitingRoomService.convertToQueue(1L);

        assertThat(count).isEqualTo(3);
        // Verify single batch ZADD call with 3 tuples
        ArgumentCaptor<Set<ZSetOperations.TypedTuple<String>>> captor =
                ArgumentCaptor.forClass(Set.class);
        verify(zSetOperations).add(eq("queue:1"), captor.capture());
        assertThat(captor.getValue()).hasSize(3);
        verify(redisTemplate).delete("waitingroom:1");
    }

    @Test
    void convertToQueue_emptyWaitingRoom_returnsZero() {
        given(redisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.members("waitingroom:1")).willReturn(Set.of());

        int count = waitingRoomService.convertToQueue(1L);

        assertThat(count).isEqualTo(0);
    }
}
