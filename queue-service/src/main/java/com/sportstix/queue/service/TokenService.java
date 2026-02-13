package com.sportstix.queue.service;

import com.sportstix.queue.config.QueueProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private static final String TOKEN_KEY_PREFIX = "queue:token:";

    private final StringRedisTemplate redisTemplate;
    private final QueueProperties queueProperties;

    public String issueToken(Long gameId, Long userId) {
        String token = UUID.randomUUID().toString();
        String key = tokenKey(gameId, userId);
        redisTemplate.opsForValue().set(key, token, Duration.ofSeconds(queueProperties.getTokenTtlSeconds()));
        return token;
    }

    public boolean validateToken(Long gameId, Long userId, String token) {
        String key = tokenKey(gameId, userId);
        String storedToken = redisTemplate.opsForValue().get(key);
        return token != null && token.equals(storedToken);
    }

    public void revokeToken(Long gameId, Long userId) {
        redisTemplate.delete(tokenKey(gameId, userId));
    }

    public boolean hasToken(Long gameId, Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey(gameId, userId)));
    }

    public String getToken(Long gameId, Long userId) {
        return redisTemplate.opsForValue().get(tokenKey(gameId, userId));
    }

    private String tokenKey(Long gameId, Long userId) {
        return TOKEN_KEY_PREFIX + gameId + ":" + userId;
    }
}
