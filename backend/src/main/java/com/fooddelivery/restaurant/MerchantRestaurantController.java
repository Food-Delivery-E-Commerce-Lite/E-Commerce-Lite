package com.fooddelivery.restaurant;

import com.fooddelivery.auth.AuthUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
public class MerchantRestaurantController {
    private final RestaurantService service;

    public MerchantRestaurantController(RestaurantService service) {
        this.service = service;
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDtos.RestaurantResponse createRestaurant(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody RestaurantDtos.RestaurantRequest request) {
        return service.createRestaurant(user.id(), request);
    }

    @GetMapping("/restaurants")
    public java.util.List<RestaurantDtos.RestaurantResponse> getRestaurants(
            @AuthenticationPrincipal AuthUser user) {
        return service.getOwnedRestaurants(user.id());
    }

    @GetMapping("/restaurants/{id}")
    public RestaurantDtos.RestaurantResponse getRestaurant(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return service.getOwnedRestaurantResponse(user.id(), id);
    }

    @PutMapping("/restaurants/{id}")
    public RestaurantDtos.RestaurantResponse updateRestaurant(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.RestaurantRequest request) {
        return service.updateRestaurant(user.id(), id, request);
    }

    @PostMapping("/restaurants/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantDtos.MenuItemResponse createMenuItem(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.MenuItemRequest request) {
        return service.createMenuItem(user.id(), id, request);
    }

    @PutMapping("/menu-items/{itemId}")
    public RestaurantDtos.MenuItemResponse updateMenuItem(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long itemId,
            @Valid @RequestBody RestaurantDtos.MenuItemRequest request) {
        return service.updateMenuItem(user.id(), itemId, request);
    }

    @PatchMapping("/menu-items/{itemId}/availability")
    public RestaurantDtos.MenuItemResponse updateAvailability(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long itemId,
            @Valid @RequestBody RestaurantDtos.AvailabilityRequest request) {
        return service.updateAvailability(user.id(), itemId, request.available());
    }
}
