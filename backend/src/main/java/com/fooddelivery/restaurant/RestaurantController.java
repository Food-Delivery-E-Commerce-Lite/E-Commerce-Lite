package com.fooddelivery.restaurant;

import com.fooddelivery.common.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class RestaurantController {
    private final RestaurantService service;
    private final ReviewService reviews;

    public RestaurantController(RestaurantService service, ReviewService reviews) {
        this.service = service;
        this.reviews = reviews;
    }

    @GetMapping("/restaurants")
    public ApiResponse<List<RestaurantDtos.RestaurantResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(service.search(keyword, categoryId, minRating, sort, page, size));
    }

    @GetMapping("/restaurants/{id}")
    public ApiResponse<RestaurantDtos.RestaurantResponse> getRestaurant(@PathVariable Long id) {
        return ApiResponse.ok(service.getPublicRestaurant(id));
    }

    @GetMapping("/restaurants/{id}/menu")
    public ApiResponse<List<RestaurantDtos.MenuItemResponse>> getMenu(@PathVariable Long id) {
        return ApiResponse.ok(service.getPublicMenu(id));
    }

    @GetMapping("/categories")
    public ApiResponse<List<RestaurantDtos.CategoryResponse>> getCategories() {
        return ApiResponse.ok(service.getCategories());
    }

    @GetMapping("/restaurants/{id}/reviews")
    public ApiResponse<List<ReviewDtos.ReviewResponse>> getReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(reviews.listByRestaurant(id, page, size));
    }
}
