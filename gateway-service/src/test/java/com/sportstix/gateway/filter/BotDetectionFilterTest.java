package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BotDetectionFilterTest {

    private BotPreventionProperties properties;
    private BotDetectionFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new BotPreventionProperties();
        properties.getFingerprint().setEnabled(true);
        properties.getFingerprint().setBlockedUserAgentPatterns(List.of(
                "(?i).*curl.*",
                "(?i).*selenium.*",
                "(?i).*python-requests.*"
        ));
        filter = new BotDetectionFilter(properties);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void validUserAgent_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 Chrome/120")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingUserAgent_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void blankUserAgent_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "   ")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void curlUserAgent_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "curl/7.88.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void seleniumUserAgent_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 Selenium/4.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void pythonRequestsUserAgent_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "python-requests/2.31.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void disabledFingerprint_shouldPassThrough() {
        properties.getFingerprint().setEnabled(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/queue/enter").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    void orderShouldBeMinusNinety() {
        assertThat(filter.getOrder()).isEqualTo(-90);
    }
}
