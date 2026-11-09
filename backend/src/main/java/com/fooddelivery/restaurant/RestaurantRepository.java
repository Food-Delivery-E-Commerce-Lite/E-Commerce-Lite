package com.fooddelivery.restaurant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, JpaSpecificationExecutor<Restaurant> {
    Page<Restaurant> findByStatus(RestaurantStatus status, Pageable pageable);
    Page<Restaurant> findByStatusAndNameContainingIgnoreCase(
        RestaurantStatus status, String keyword, Pageable pageable);
    Page<Restaurant> findByOwnerId(Long ownerId, Pageable pageable);
    default Page<Restaurant> search(Specification<Restaurant> spec, Pageable pageable) {
        return findAll(spec, pageable);
    }
}
