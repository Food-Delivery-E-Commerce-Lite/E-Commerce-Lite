package com.fooddelivery.order;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order changeStatus(Long orderId, OrderStatus next) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.changeStatus(next);
        return order;
    }

    // TODO: placeOrder(customerId, cart, addressId), cancelOrder, listMyOrders
}
