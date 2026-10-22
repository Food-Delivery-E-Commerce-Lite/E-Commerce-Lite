package com.fooddelivery.auth;

import com.fooddelivery.auth.AuthDtos.*;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.user.Role;
import com.fooddelivery.user.User;
import com.fooddelivery.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
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
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
            .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
            .orElseThrow(() -> new BusinessException("INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED));
        if (!user.isActive()) {
            throw new BusinessException("ACCOUNT_DISABLED", "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN);
        }
        return toResponse(user);
    }

    private AuthResponse toResponse(User u) {
        return new AuthResponse(jwt.generateToken(u), "Bearer", jwt.getExpirationMinutes(),
            new UserInfo(u.getId(), u.getEmail(), u.getFullName(), u.getRole()));
    }
}
