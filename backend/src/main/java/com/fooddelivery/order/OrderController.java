package com.fooddelivery.order;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderDtos.OrderResponse> placeOrder(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody OrderDtos.PlaceOrderRequest body) {
        return ApiResponse.ok(orderService.placeOrder(user.id(), body));
    }

    @GetMapping("/orders")
    public ApiResponse<java.util.List<OrderDtos.OrderResponse>> myOrders(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OrderDtos.OrderResponse> p = orderService.listMyOrders(
            user.id(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return ApiResponse.page(p);
    }

    @GetMapping("/orders/{id}")
    public ApiResponse<OrderDtos.OrderResponse> getOrder(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.getMyOrder(user.id(), id));
    }

    @PostMapping("/orders/{id}/cancel")
    public ApiResponse<OrderDtos.OrderResponse> cancelOrder(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(orderService.cancelMyOrder(user.id(), id));
    }

    @GetMapping("/orders/{id}/history")
    public ApiResponse<java.util.List<OrderStatusHistory>> myHistory(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        // Chỉ chủ đơn hoặc chủ quán mới được xem
        return ApiResponse.ok(orderService.getHistoryFor(user, id));
    }
}
