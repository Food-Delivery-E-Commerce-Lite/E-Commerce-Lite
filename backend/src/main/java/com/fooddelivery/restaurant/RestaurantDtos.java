package com.fooddelivery.restaurant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;

public final class RestaurantDtos {
    private RestaurantDtos() {}

    public record RestaurantRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 250) String address,
        LocalTime openTime,
        LocalTime closeTime
    ) {}

    public record RestaurantResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        String address,
        LocalTime openTime,
        LocalTime closeTime,
        RestaurantStatus status,
        BigDecimal averageRating,
        int ratingCount
    ) {
        static RestaurantResponse from(Restaurant restaurant) {
            return new RestaurantResponse(restaurant.getId(), restaurant.getOwnerId(),
                restaurant.getName(), restaurant.getDescription(), restaurant.getAddress(),
                restaurant.getOpenTime(), restaurant.getCloseTime(), restaurant.getStatus(),
                restaurant.getAverageRating(), restaurant.getRatingCount());
        }
    }

    public record RestaurantStatusRequest(@NotNull RestaurantStatus status) {}

    public record MenuItemRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 4000) String description,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal price,
        @Size(max = 300) String imageUrl,
        Long categoryId
    ) {}

    public record AvailabilityRequest(@NotNull Boolean available) {}

    public record MenuItemResponse(
        Long id,
        Long restaurantId,
        Long categoryId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        boolean available
    ) {
        static MenuItemResponse from(MenuItem item) {
            return new MenuItemResponse(item.getId(), item.getRestaurantId(), item.getCategoryId(),
                item.getName(), item.getDescription(), item.getPrice(), item.getImageUrl(),
                item.isAvailable());
        }
    }

    public record CategoryResponse(Long id, String name) {
        static CategoryResponse from(Category category) {
            return new CategoryResponse(category.getId(), category.getName());
        }
    }
}
