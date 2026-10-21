package com.fooddelivery.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record StatusRequest(@NotNull OrderStatus status) {}
    public record OrderResponse(Long id, OrderStatus status) {}

    // TODO(auth): chỉ cho phép role MERCHANT sở hữu nhà hàng của đơn
    @PatchMapping("/merchant/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest body) {
        Order order = orderService.changeStatus(id, body.status());
        return new OrderResponse(order.getId(), order.getStatus());
    }
}
