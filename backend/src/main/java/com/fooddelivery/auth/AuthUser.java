package com.fooddelivery.auth;

import com.fooddelivery.user.Role;

/** Thông tin người dùng hiện tại, lấy từ JWT và đặt vào SecurityContext. */
public record AuthUser(Long id, String email, Role role) {}
