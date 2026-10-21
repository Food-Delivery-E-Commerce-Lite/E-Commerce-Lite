package com.fooddelivery.order;

import java.util.Map;
import java.util.Set;

/** Máy trạng thái đơn hàng: mỗi trạng thái chỉ được chuyển tới các trạng thái hợp lệ. */
public enum OrderStatus {
    PENDING, CONFIRMED, PREPARING, DELIVERING, COMPLETED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
        PENDING,    Set.of(CONFIRMED, CANCELLED),
        CONFIRMED,  Set.of(PREPARING, CANCELLED),
        PREPARING,  Set.of(DELIVERING),
        DELIVERING, Set.of(COMPLETED),
        COMPLETED,  Set.of(),
        CANCELLED,  Set.of());

    public boolean canTransitionTo(OrderStatus next) {
        return ALLOWED.get(this).contains(next);
    }

    public boolean isFinal() {
        return ALLOWED.get(this).isEmpty();
    }
}
