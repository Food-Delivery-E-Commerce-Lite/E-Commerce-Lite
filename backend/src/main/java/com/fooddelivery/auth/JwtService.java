package com.fooddelivery.auth;

import com.fooddelivery.user.Role;
import com.fooddelivery.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessExpirationMinutes;
    private final long refreshExpirationDays;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-expiration-minutes:15}") long accessExpirationMinutes,
                      @Value("${app.jwt.refresh-expiration-days:7}") long refreshExpirationDays) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret phải dài ít nhất 32 byte (đặt JWT_SECRET trong .env)");
        }
        if (secret.startsWith("dev-only-secret")) {
            // Cảnh báo sớm để không mang secret mẫu lên production
            org.slf4j.LoggerFactory.getLogger(JwtService.class)
                .warn("Đang dùng JWT secret mặc định — hãy đặt JWT_SECRET riêng trước khi deploy production!");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMinutes = accessExpirationMinutes;
        this.refreshExpirationDays = refreshExpirationDays;
    }

    public String generateToken(User user) {
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
            .subject(String.valueOf(user.getId()))
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(accessExpirationMinutes * 60)))
            .signWith(key)
            .compact();
    }

    /** Trả về AuthUser nếu token hợp lệ, ném JwtException nếu sai hoặc hết hạn. */
    public AuthUser parse(String token) throws JwtException {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthUser(Long.valueOf(c.getSubject()),
                            c.get("email", String.class),
                            Role.valueOf(c.get("role", String.class)));
    }

    public long getExpirationMinutes() { return accessExpirationMinutes; }
    public long getRefreshExpirationDays() { return refreshExpirationDays; }
}
