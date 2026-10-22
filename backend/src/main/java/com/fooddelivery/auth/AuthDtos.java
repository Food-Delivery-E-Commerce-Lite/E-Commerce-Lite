package com.fooddelivery.auth;

import com.fooddelivery.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 8, max = 72, message = "phải từ 8 đến 72 ký tự") String password,
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 20) String phone,
        @NotNull Role role) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record UserInfo(Long id, String email, String fullName, Role role) {}

    public record AuthResponse(String accessToken, String tokenType, long expiresInMinutes, UserInfo user) {}
}
