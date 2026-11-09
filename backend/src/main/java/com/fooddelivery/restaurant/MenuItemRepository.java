package com.fooddelivery.restaurant;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantIdAndAvailableTrue(Long restaurantId);
    List<MenuItem> findByRestaurantId(Long restaurantId);
    Page<MenuItem> findByRestaurantId(Long restaurantId, Pageable pageable);
    long countByRestaurantId(Long restaurantId);
}
