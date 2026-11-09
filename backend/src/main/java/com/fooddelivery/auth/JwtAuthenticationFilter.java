package com.fooddelivery.auth;

import com.fooddelivery.user.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Đọc header "Authorization: Bearer <token>", nếu hợp lệ và user còn active thì đăng nhập cho request này. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AuthUser parsed = jwtService.parse(header.substring(7));
                var userOpt = users.findById(parsed.id());
                boolean allowed = userOpt.map(u -> u.isActive()).orElse(false);
                if (allowed) {
                    var u = userOpt.get();
                    AuthUser full = new AuthUser(u.getId(), u.getEmail(), u.getRole(), u.isMustChangePassword());
                    var auth = new UsernamePasswordAuthenticationToken(
                        full, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // Gắn cờ để frontend ép đổi mật khẩu
                    request.setAttribute("mustChangePassword", u.isMustChangePassword());
                } else {
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
