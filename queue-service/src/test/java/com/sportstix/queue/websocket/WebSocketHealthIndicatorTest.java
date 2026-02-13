package com.sportstix.queue.websocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketHealthIndicatorTest {

    @InjectMocks
    private WebSocketHealthIndicator healthIndicator;

    @Mock
    private WebSocketSessionRegistry sessionRegistry;

    @Test
    void health_belowThreshold_returnsUp() {
        given(sessionRegistry.getTotalConnections()).willReturn(500);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("totalConnections", 500);
    }

    @Test
    void health_aboveThreshold_returnsDown() {
        given(sessionRegistry.getTotalConnections()).willReturn(15_000);

        Health health = healthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("totalConnections", 15_000);
    }
}
