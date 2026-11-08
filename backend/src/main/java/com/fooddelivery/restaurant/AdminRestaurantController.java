package com.fooddelivery.restaurant;

import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/restaurants")
public class AdminRestaurantController {
    private final RestaurantService service;

    public AdminRestaurantController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RestaurantDtos.RestaurantResponse>> getRestaurants(
            @RequestParam(defaultValue = "PENDING") RestaurantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<RestaurantDtos.RestaurantResponse> p = service.getRestaurantsByStatus(status, page, size);
        return ApiResponse.page(p);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RestaurantDtos.RestaurantResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.RestaurantStatusRequest request) {
        return ApiResponse.ok(service.updateStatus(id, request.status()));
    }
}
