package com.fooddelivery.auth;

import com.fooddelivery.auth.AuthDtos.*;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.user.Role;
import com.fooddelivery.user.User;
import com.fooddelivery.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final long refreshDays;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       PasswordEncoder encoder, JwtService jwt,
                       @Value("${app.jwt.refresh-expiration-days:7}") long refreshDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.jwt = jwt;
        this.refreshDays = refreshDays;
    }

    @Transactional
    public TokenPair register(RegisterRequest req) {
        if (req.role() == Role.ADMIN) {
            throw new BusinessException("ROLE_NOT_ALLOWED",
                "Không thể tự đăng ký tài khoản ADMIN", HttpStatus.BAD_REQUEST);
        }
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new BusinessException("EMAIL_EXISTS", "Email đã được sử dụng", HttpStatus.CONFLICT);
        }
        User user = new User(email, encoder.encode(req.password()), req.fullName().trim(), req.role());
        user.setPhone(req.phone());
        users.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenPair login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
            .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }
        return issueTokens(user);
    }

    /** Xoay vòng refresh token: thu hồi token cũ, cấp cặp mới. */
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        String hash = sha256(rawRefreshToken);
        RefreshToken stored = refreshTokens.findByTokenHash(hash)
            .orElseThrow(() -> new BusinessException("INVALID_REFRESH",
                "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED));
        if (stored.isRevoked() || stored.isExpired()) {
            // Token đã xoay vòng mà bị dùng lại -> nghi bị đánh cắp: đá hết mọi phiên cho an toàn
            if (stored.isRevoked()) {
                try { refreshTokens.revokeAllByUser(stored.getUserId()); } catch (Exception ignored) { }
            } else {
                stored.revoke();
            }
            throw new BusinessException("INVALID_REFRESH",
                "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED);
        }
        User user = users.findById(stored.getUserId())
            .orElseThrow(() -> new BusinessException("INVALID_REFRESH",
                "Tài khoản không tồn tại", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }
        // Thu hồi token cũ trước khi cấp mới (chống replay)
        stored.revoke();
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null) return;
        refreshTokens.findByTokenHash(sha256(rawRefreshToken)).ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokens.revokeAllByUser(userId);
    }

    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest req) {
        User user = users.findById(userId)
            .orElseThrow(() -> new BusinessException("USER_NOT_FOUND",
                "Tài khoản không tồn tại", HttpStatus.NOT_FOUND));
        if (!encoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new BusinessException("WRONG_PASSWORD",
                "Mật khẩu cũ không đúng", HttpStatus.BAD_REQUEST);
        }
        if (encoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("SAME_PASSWORD",
                "Mật khẩu mới phải khác mật khẩu cũ", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        user.setMustChangePassword(false);
        users.save(user);
        // Đổi mật khẩu thì đá hết phiên cũ
        refreshTokens.revokeAllByUser(userId);
        TokenPair pair = issueTokens(user);
        return toResponse(user, pair.accessToken());
    }

    public record TokenPair(String accessToken, String refreshToken, User user) {}

    private TokenPair issueTokens(User user) {
        String access = jwt.generateAccessToken(user);
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(refreshDays, ChronoUnit.DAYS);
        refreshTokens.save(new RefreshToken(user.getId(), sha256(rawRefresh), expiresAt));
        return new TokenPair(access, rawRefresh, user);
    }

    private AuthResponse toResponse(User u, String access) {
        return new AuthResponse(access, "Bearer", jwt.getExpirationMinutes(),
            new UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isMustChangePassword()));
    }

    public AuthResponse toResponse(User u) {
        return toResponse(u, jwt.generateAccessToken(u));
    }

    public long accessMinutes() { return jwt.getExpirationMinutes(); }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
