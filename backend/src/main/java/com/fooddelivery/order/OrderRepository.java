package com.fooddelivery.order;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    List<Order> findByRestaurantIdAndStatus(Long restaurantId, OrderStatus status);
    Page<Order> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
    List<Order> findByRestaurantId(Long restaurantId);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.restaurantId = :restaurantId and o.status <> 'CANCELLED'")
    java.math.BigDecimal sumRevenueByRestaurant(Long restaurantId);

    @Query("select count(o) from Order o where o.restaurantId = :restaurantId")
    long countByRestaurant(Long restaurantId);
}
