package com.sportstix.gateway.filter;

import com.sportstix.gateway.config.BotPreventionProperties;
import com.sportstix.gateway.service.CaptchaVerificationService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CaptchaValidationFilterTest {

    private BotPreventionProperties properties;
    private CaptchaVerificationService captchaService;
    private CaptchaValidationFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new BotPreventionProperties();
        properties.getCaptcha().setEnabled(true);
        properties.getCaptcha().setProtectedPaths(List.of(
                "/api/v1/queue/enter",
                "/api/v1/queue/waiting-room/register"
        ));

        captchaService = mock(CaptchaVerificationService.class);
        filter = new CaptchaValidationFilter(properties, captchaService);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void protectedPath_withValidToken_shouldPassThrough() {
        when(captchaService.verify("valid-token")).thenReturn(Mono.just(true));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header("X-Captcha-Token", "valid-token")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verify(captchaService).verify("valid-token");
    }

    @Test
    void protectedPath_withoutToken_shouldReturn403() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
        verifyNoInteractions(captchaService);
    }

    @Test
    void protectedPath_withInvalidToken_shouldReturn403() {
        when(captchaService.verify("bad-token")).thenReturn(Mono.just(false));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header("X-Captcha-Token", "bad-token")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void nonProtectedPath_shouldPassThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/games")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(captchaService);
    }

    @Test
    void disabledCaptcha_shouldPassThrough() {
        properties.getCaptcha().setEnabled(false);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/enter")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
        verifyNoInteractions(captchaService);
    }

    @Test
    void waitingRoomRegister_shouldRequireCaptcha() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/queue/waiting-room/register")
                        .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                        .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void orderShouldBeMinusSeventy() {
        assertThat(filter.getOrder()).isEqualTo(-70);
    }
}
