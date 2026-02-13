package com.sportstix.queue.service;

import com.sportstix.queue.config.QueueProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    private TokenService tokenService;

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private QueueProperties queueProperties;

    @BeforeEach
    void setUp() {
        queueProperties = new QueueProperties();
        queueProperties.setTokenTtlSeconds(600);
        org.mockito.Mockito.lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenService = new TokenService(redisTemplate, queueProperties);
    }

    @Test
    void issueToken_storesWithTTL() {
        String token = tokenService.issueToken(1L, 100L);

        assertThat(token).isNotBlank();
        verify(valueOperations).set(eq("queue:token:1:100"), anyString(), eq(Duration.ofSeconds(600)));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        given(valueOperations.get("queue:token:1:100")).willReturn("valid-token");

        boolean result = tokenService.validateToken(1L, 100L, "valid-token");

        assertThat(result).isTrue();
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        given(valueOperations.get("queue:token:1:100")).willReturn("valid-token");

        boolean result = tokenService.validateToken(1L, 100L, "wrong-token");

        assertThat(result).isFalse();
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        boolean result = tokenService.validateToken(1L, 100L, null);

        assertThat(result).isFalse();
    }

    @Test
    void revokeToken_deletesKey() {
        tokenService.revokeToken(1L, 100L);

        verify(redisTemplate).delete("queue:token:1:100");
    }

    @Test
    void hasToken_exists_returnsTrue() {
        given(redisTemplate.hasKey("queue:token:1:100")).willReturn(true);

        boolean result = tokenService.hasToken(1L, 100L);

        assertThat(result).isTrue();
    }
}
