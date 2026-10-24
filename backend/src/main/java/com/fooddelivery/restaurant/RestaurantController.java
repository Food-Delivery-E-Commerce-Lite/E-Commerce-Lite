package com.fooddelivery.restaurant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.data.domain.Page;
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

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @GetMapping("/restaurants")
    public Page<RestaurantDtos.RestaurantResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(keyword, page, size);
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantDtos.RestaurantResponse getRestaurant(@PathVariable Long id) {
        return service.getPublicRestaurant(id);
    }

    @GetMapping("/restaurants/{id}/menu")
    public List<RestaurantDtos.MenuItemResponse> getMenu(@PathVariable Long id) {
        return service.getPublicMenu(id);
    }

    @GetMapping("/categories")
    public List<RestaurantDtos.CategoryResponse> getCategories() {
        return service.getCategories();
    }
}
