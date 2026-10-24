package com.fooddelivery.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
    public Page<RestaurantDtos.RestaurantResponse> getRestaurants(
            @RequestParam(defaultValue = "PENDING") RestaurantStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.getRestaurantsByStatus(status, page, size);
    }

    @PatchMapping("/{id}/status")
    public RestaurantDtos.RestaurantResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.RestaurantStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}
