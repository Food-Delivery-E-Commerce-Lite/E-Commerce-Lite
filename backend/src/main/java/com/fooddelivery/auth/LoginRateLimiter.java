package com.fooddelivery.auth;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.fooddelivery.common.exception.BusinessException;

/** Rate-limit đơn giản theo IP cho login/register: 10 lần/phút. */
@Service
public class LoginRateLimiter {
    private final int maxAttempts;
    private final long windowSeconds;
    private final Map<String, Attempt> buckets = new ConcurrentHashMap<>();

    private record Attempt(int count, long windowStart) {}

    public LoginRateLimiter(
            @Value("${app.security.login-max-attempts:10}") int maxAttempts,
            @Value("${app.security.login-window-seconds:60}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
    }

    public void check(String key) {
        long now = Instant.now().getEpochSecond();
        buckets.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart() >= windowSeconds) {
                return new Attempt(1, now);
            }
            if (old.count() >= maxAttempts) {
                throw new BusinessException("TOO_MANY_REQUESTS",
                    "Bạn thử quá nhiều lần, vui lòng đợi 1 phút", HttpStatus.TOO_MANY_REQUESTS);
            }
            return new Attempt(old.count() + 1, old.windowStart());
        });
    }
}
