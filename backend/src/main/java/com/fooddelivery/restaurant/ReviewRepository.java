package com.fooddelivery.restaurant;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
    boolean existsByOrderId(Long orderId);
    List<Review> findByRestaurantId(Long restaurantId);
}
