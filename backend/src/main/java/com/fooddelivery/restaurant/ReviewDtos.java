package com.fooddelivery.restaurant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ReviewDtos {
    private ReviewDtos() {}

    public record CreateReviewRequest(
        @NotNull Long orderId,
        @Min(1) @Max(5) int rating,
        @Size(max = 4000) String comment) {}

    public record ReviewResponse(
        Long id, Long orderId, Long userId, Long restaurantId,
        int rating, String comment, Instant createdAt) {
        static ReviewResponse from(Review r) {
            return new ReviewResponse(r.getId(), r.getOrderId(), r.getUserId(),
                r.getRestaurantId(), r.getRating(), r.getComment(), r.getCreatedAt());
        }
    }
}
