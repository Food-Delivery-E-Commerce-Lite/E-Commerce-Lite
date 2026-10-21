package com.fooddelivery.order;

import static org.junit.jupiter.api.Assertions.*;

import com.fooddelivery.common.exception.InvalidOrderStateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

    @ParameterizedTest
    @CsvSource({
        "PENDING,CONFIRMED", "PENDING,CANCELLED", "CONFIRMED,PREPARING",
        "PREPARING,DELIVERING", "DELIVERING,COMPLETED"})
    void validTransitions(OrderStatus from, OrderStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest
    @CsvSource({"PENDING,DELIVERING", "COMPLETED,PREPARING", "CANCELLED,PENDING", "PREPARING,CANCELLED"})
    void invalidTransitions(OrderStatus from, OrderStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void finalStates() {
        assertTrue(OrderStatus.COMPLETED.isFinal());
        assertTrue(OrderStatus.CANCELLED.isFinal());
        assertFalse(OrderStatus.PENDING.isFinal());
    }

    @Test
    void orderChangeStatus_invalidThrows() {
        Order o = new Order(1L, 1L, 1L, new BigDecimal("100000"), new BigDecimal("15000"));
        assertEquals(new BigDecimal("115000"), o.getTotalAmount());
        assertThrows(InvalidOrderStateException.class, () -> o.changeStatus(OrderStatus.DELIVERING));
        o.changeStatus(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, o.getStatus());
    }
}
