package com.fooddelivery.auth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanup {
    private final RefreshTokenRepository refreshTokens;

    public RefreshTokenCleanup(RefreshTokenRepository refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void clean() {
        refreshTokens.deleteExpiredOrRevoked();
    }
}
