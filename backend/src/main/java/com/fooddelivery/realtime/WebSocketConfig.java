package com.fooddelivery.realtime;

import com.fooddelivery.auth.JwtService;
import com.fooddelivery.order.OrderRepository;
import com.fooddelivery.restaurant.RestaurantRepository;
import com.fooddelivery.user.Role;
import com.fooddelivery.user.UserRepository;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final String allowedOrigins;
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final UserRepository users;

    public WebSocketConfig(JwtService jwtService,
                           @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins,
                           OrderRepository orders, RestaurantRepository restaurants, UserRepository users) {
        this.jwtService = jwtService;
        this.allowedOrigins = allowedOrigins;
        this.orders = orders;
        this.restaurants = restaurants;
        this.users = users;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
            .addInterceptors(new HandshakeInterceptor() {
                @Override
                public boolean beforeHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                               WebSocketHandler h, Map<String, Object> attrs) {
                    // Cho phép handshake luôn, auth thực hiện ở CONNECT (STOMP) để hỗ trợ SockJS
                    if (req instanceof ServletServerHttpRequest servlet) {
                        String token = servlet.getServletRequest().getParameter("token");
                        if (token != null && !token.isBlank()) {
                            try {
                                var auth = jwtService.parse(token);
                                attrs.put("authUser", auth);
                            } catch (Exception ignored) { }
                        }
                    }
                    return true;
                }
                @Override
                public void afterHandshake(ServerHttpRequest req, ServerHttpResponse res,
                                           WebSocketHandler h, Exception ex) { }
            })
            .setAllowedOriginPatterns(allowedOrigins.split(","))
            .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (acc == null) return message;
                if (StompCommand.CONNECT.equals(acc.getCommand())) {
                    String header = acc.getFirstNativeHeader("Authorization");
                    String token = null;
                    if (header != null && header.startsWith("Bearer ")) {
                        token = header.substring(7);
                    } else {
                        // Fallback: token gửi trong header 'token' (cho SockJS đơn giản)
                        token = acc.getFirstNativeHeader("token");
                    }
                    com.fooddelivery.auth.AuthUser authUser = null;
                    if (token != null) {
                        try {
                            authUser = jwtService.parse(token);
                        } catch (Exception ignored) { }
                    } else {
                        // Thử lấy từ attributes handshake (query ?token=)
                        Object attr = acc.getSessionAttributes() != null
                            ? acc.getSessionAttributes().get("authUser") : null;
                        if (attr instanceof com.fooddelivery.auth.AuthUser a) authUser = a;
                    }
                    if (authUser != null) {
                        // Chặn token của tài khoản đã bị khóa ngay từ lúc CONNECT
                        var u = users.findById(authUser.id()).orElse(null);
                        if (u == null || !u.isActive()) return message;
                        var full = new com.fooddelivery.auth.AuthUser(
                            u.getId(), u.getEmail(), u.getRole(), u.isMustChangePassword());
                        var auth = new UsernamePasswordAuthenticationToken(
                            full, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                        acc.setUser(auth);
                    }
                    return message;
                }
                if (StompCommand.SUBSCRIBE.equals(acc.getCommand())) {
                    // Chặn nghe trộm: chỉ chủ đơn / chủ quán / ADMIN mới được sub topic đơn/quán
                    var principal = acc.getUser();
                    com.fooddelivery.auth.AuthUser me = null;
                    if (principal instanceof UsernamePasswordAuthenticationToken t
                            && t.getPrincipal() instanceof com.fooddelivery.auth.AuthUser a) {
                        me = a;
                    }
                    if (me == null) {
                        throw new org.springframework.messaging.MessageDeliveryException(
                            "SUBSCRIBE_DENIED: cần đăng nhập realtime");
                    }
                    String dest = acc.getDestination();
                    if (dest != null && dest.startsWith("/topic/orders/")) {
                        Long orderId = parseTrailingId(dest);
                        if (orderId == null) throw new org.springframework.messaging.MessageDeliveryException("SUBSCRIBE_DENIED");
                        var order = orders.findById(orderId).orElse(null);
                        if (order == null) throw new org.springframework.messaging.MessageDeliveryException("SUBSCRIBE_DENIED");
                        if (!isOrderVisible(me, order)) {
                            throw new org.springframework.messaging.MessageDeliveryException(
                                "SUBSCRIBE_DENIED: không có quyền nghe đơn #" + orderId);
                        }
                    } else if (dest != null && dest.startsWith("/topic/merchant/")) {
                        Long rid = parseTrailingId(dest);
                        if (rid == null) throw new org.springframework.messaging.MessageDeliveryException("SUBSCRIBE_DENIED");
                        var r = restaurants.findById(rid).orElse(null);
                        if (r == null) throw new org.springframework.messaging.MessageDeliveryException("SUBSCRIBE_DENIED");
                        boolean owner = r.getOwnerId() != null && r.getOwnerId().equals(me.id());
                        if (!owner && me.role() != Role.ADMIN) {
                            throw new org.springframework.messaging.MessageDeliveryException(
                                "SUBSCRIBE_DENIED: không có quyền nghe quán #" + rid);
                        }
                    }
                }
                return message;
            }

            private Long parseTrailingId(String dest) {
                try {
                    String tail = dest.substring(dest.lastIndexOf('/') + 1);
                    return Long.valueOf(tail);
                } catch (Exception e) {
                    return null;
                }
            }

            private boolean isOrderVisible(com.fooddelivery.auth.AuthUser me, com.fooddelivery.order.Order order) {
                if (order.ownedBy(me.id())) return true;
                if (me.role() == Role.ADMIN) return true;
                var r = restaurants.findById(order.getRestaurantId()).orElse(null);
                return r != null && r.getOwnerId() != null && r.getOwnerId().equals(me.id());
            }
        });
    }
}
