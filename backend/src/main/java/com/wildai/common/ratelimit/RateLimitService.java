package com.wildai.common.ratelimit;

import com.wildai.common.exception.BusinessException;
import com.wildai.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void check(String key, int maxAttempts, Duration window) {
        try {
            Long count = redis.opsForValue().increment("rl:" + key);
            if (count != null && count == 1L) {
                redis.expire("rl:" + key, window);
            }
            if (count != null && count > maxAttempts) {
                throw new BusinessException(ErrorCode.RATE_LIMITED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            // Redis 不可用时跳过限流
        }
    }
}
