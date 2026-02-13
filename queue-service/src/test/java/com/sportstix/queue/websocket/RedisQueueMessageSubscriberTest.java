package com.sportstix.queue.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sportstix.queue.dto.response.QueueUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RedisQueueMessageSubscriberTest {

    private RedisQueueMessageSubscriber subscriber;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        subscriber = new RedisQueueMessageSubscriber(messagingTemplate, objectMapper);
    }

    @Test
    void onMessage_waitingUpdate_sendsToUserAndGameTopics() throws Exception {
        QueueUpdateMessage message = QueueUpdateMessage.waiting(1L, 100L, 5, 50, 12);
        String json = objectMapper.writeValueAsString(message);

        subscriber.onMessage(json);

        // Verify user-specific topic
        ArgumentCaptor<QueueUpdateMessage> captor = ArgumentCaptor.forClass(QueueUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/queue/1/100"),
                captor.capture()
        );
        assertThat(captor.getValue().rank()).isEqualTo(5);

        // Verify game-level topic
        verify(messagingTemplate).convertAndSend(
                eq("/topic/queue/1"),
                captor.capture()
        );
    }

    @Test
    void onMessage_eligibleUpdate_sendsTokenToUser() throws Exception {
        QueueUpdateMessage message = QueueUpdateMessage.eligible(1L, 200L, "entrance-token");
        String json = objectMapper.writeValueAsString(message);

        subscriber.onMessage(json);

        ArgumentCaptor<QueueUpdateMessage> captor = ArgumentCaptor.forClass(QueueUpdateMessage.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/queue/1/200"),
                captor.capture()
        );
        assertThat(captor.getValue().status()).isEqualTo("ELIGIBLE");
        assertThat(captor.getValue().token()).isEqualTo("entrance-token");
    }

    @Test
    void onMessage_invalidJson_doesNotThrow() {
        subscriber.onMessage("invalid-json");

        verify(messagingTemplate, times(0)).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(QueueUpdateMessage.class)
        );
    }
}
