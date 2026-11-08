package com.fooddelivery.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);
    Page<Restaurant> findByStatusAndNameContainingIgnoreCase(
        RestaurantStatus status, String keyword, Pageable pageable);
    Page<Restaurant> findByOwnerId(Long ownerId, Pageable pageable);
}
