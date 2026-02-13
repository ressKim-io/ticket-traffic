package com.sportstix.queue.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstix.queue.config.RedisPubSubConfig;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueueBroadcastServiceTest {

    private QueueBroadcastService broadcastService;

    @Mock
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        broadcastService = new QueueBroadcastService(redisTemplate, objectMapper);
    }

    @Test
    void broadcastUpdate_waiting_publishesJson() throws Exception {
        QueueUpdateMessage message = QueueUpdateMessage.waiting(1L, 100L, 5, 50, 12);

        broadcastService.broadcastUpdate(message);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(
                eq(RedisPubSubConfig.QUEUE_UPDATE_CHANNEL),
                captor.capture()
        );

        String json = captor.getValue();
        QueueUpdateMessage deserialized = objectMapper.readValue(json, QueueUpdateMessage.class);
        assertThat(deserialized.gameId()).isEqualTo(1L);
        assertThat(deserialized.userId()).isEqualTo(100L);
        assertThat(deserialized.status()).isEqualTo("WAITING");
        assertThat(deserialized.rank()).isEqualTo(5);
    }

    @Test
    void broadcastUpdate_eligible_publishesJson() throws Exception {
        QueueUpdateMessage message = QueueUpdateMessage.eligible(1L, 100L, "token-abc");

        broadcastService.broadcastUpdate(message);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(
                eq(RedisPubSubConfig.QUEUE_UPDATE_CHANNEL),
                captor.capture()
        );

        QueueUpdateMessage deserialized = objectMapper.readValue(captor.getValue(), QueueUpdateMessage.class);
        assertThat(deserialized.status()).isEqualTo("ELIGIBLE");
        assertThat(deserialized.token()).isEqualTo("token-abc");
    }
}
