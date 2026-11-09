package com.fooddelivery.auth;

import com.fooddelivery.auth.AuthDtos.*;
import com.fooddelivery.common.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;
    private final long refreshDays;
    private final boolean cookieSecure;

    public AuthController(AuthService authService, LoginRateLimiter rateLimiter,
                          @Value("${app.jwt.refresh-expiration-days:7}") long refreshDays,
                          @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
        this.refreshDays = refreshDays;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req,
                                              HttpServletRequest httpReq, HttpServletResponse httpRes) {
        rateLimiter.check(clientKey(httpReq));
        AuthService.TokenPair pair = authService.register(req);
        setRefreshCookie(httpRes, pair.refreshToken());
        return ApiResponse.ok(toResponse(pair));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req,
                                           HttpServletRequest httpReq, HttpServletResponse httpRes) {
        rateLimiter.check(clientKey(httpReq));
        AuthService.TokenPair pair = authService.login(req);
        setRefreshCookie(httpRes, pair.refreshToken());
        return ApiResponse.ok(toResponse(pair));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        String raw = readRefreshCookie(httpReq);
        if (raw == null) {
            throw new com.fooddelivery.common.exception.BusinessException("INVALID_REFRESH",
                "Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", HttpStatus.UNAUTHORIZED);
        }
        AuthService.TokenPair pair = authService.refresh(raw);
        setRefreshCookie(httpRes, pair.refreshToken());
        return ApiResponse.ok(toResponse(pair));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        authService.logout(readRefreshCookie(httpReq));
        clearRefreshCookie(httpRes);
    }

    @PostMapping("/change-password")
    public ApiResponse<AuthResponse> changePassword(@AuthenticationPrincipal AuthUser user,
                                                    @Valid @RequestBody ChangePasswordRequest req,
                                                    HttpServletResponse httpRes) {
        AuthResponse res = authService.changePassword(user.id(), req);
        // Cấp lại refresh cookie mới sau khi đổi mật khẩu
        // (AuthService đã thu hồi hết phiên cũ, cần lấy refresh mới qua login lại ở client,
        //  ở đây clear cookie để client login lại cho sạch)
        clearRefreshCookie(httpRes);
        return ApiResponse.ok(res);
    }

    /** Endpoint này cần token, dùng để kiểm tra đăng nhập thành công. */
    @GetMapping("/me")
    public ApiResponse<AuthUser> me(@AuthenticationPrincipal AuthUser user) {
        return ApiResponse.ok(user);
    }

    private AuthResponse toResponse(AuthService.TokenPair pair) {
        var u = pair.user();
        return new AuthResponse(pair.accessToken(), "Bearer",
            authService.accessMinutes(), new UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isMustChangePassword()));
    }

    private void setRefreshCookie(HttpServletResponse res, String rawRefresh) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, rawRefresh)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSecure ? "None" : "Lax")
            .path("/api/v1/auth")
            .maxAge(Duration.ofDays(refreshDays))
            .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite(cookieSecure ? "None" : "Lax")
            .path("/api/v1/auth")
            .maxAge(0)
            .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private static String readRefreshCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return null;
        return Arrays.stream(req.getCookies())
            .filter(c -> REFRESH_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst().orElse(null);
    }

    private static String clientKey(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
