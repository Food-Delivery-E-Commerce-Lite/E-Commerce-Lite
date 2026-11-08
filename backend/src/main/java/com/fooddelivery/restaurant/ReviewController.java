package com.fooddelivery.restaurant;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    private final ReviewService reviews;

    public ReviewController(ReviewService reviews) {
        this.reviews = reviews;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewDtos.ReviewResponse> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody ReviewDtos.CreateReviewRequest req) {
        return ApiResponse.ok(reviews.create(user.id(), req));
    }
}
