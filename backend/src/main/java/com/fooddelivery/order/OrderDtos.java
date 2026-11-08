package com.fooddelivery.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record CartLine(@NotNull Long itemId, @Min(1) @Max(50) int quantity) {}

    public record PlaceOrderRequest(
        @NotNull Long restaurantId,
        @NotNull Long addressId,
        @NotEmpty List<CartLine> items,
        Payment.Method method) {
        public PlaceOrderRequest {
            if (method == null) method = Payment.Method.COD;
        }
    }

    public record OrderItemView(Long itemId, String itemName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {
        static OrderItemView from(OrderItem i) {
            return new OrderItemView(i.getItemId(), i.getItemName(), i.getUnitPrice(), i.getQuantity(), i.lineTotal());
        }
    }

    public record OrderResponse(
        Long id, Long restaurantId, Long addressId, OrderStatus status,
        BigDecimal subtotal, BigDecimal deliveryFee, BigDecimal totalAmount,
        Payment.Method payMethod, Payment.Status payStatus,
        List<OrderItemView> items, Instant createdAt) {}

    public record StatusRequest(@NotNull OrderStatus status) {}
    public record CancelRequest(String reason) {}
}
