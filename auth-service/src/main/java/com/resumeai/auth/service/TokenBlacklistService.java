package com.resumeai.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {

    private static final String PREFIX = "auth:blacklist:";
    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String token, long ttlMillis) {
        if (token == null || token.isBlank() || ttlMillis <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(PREFIX + token, "1", Duration.ofMillis(ttlMillis));
    }

    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}
