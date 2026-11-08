package com.fooddelivery.restaurant;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import com.fooddelivery.order.OrderRepository;
import com.fooddelivery.order.OrderStatus;
import com.fooddelivery.order.OrderDtos;
import com.fooddelivery.order.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public MerchantRestaurantController(RestaurantService service,
                                        OrderRepository orderRepository,
                                        OrderService orderService) {
        this.service = service;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @PostMapping("/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantDtos.RestaurantResponse> createRestaurant(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody RestaurantDtos.RestaurantRequest request) {
        return ApiResponse.ok(service.createRestaurant(user.id(), request));
    }

    @GetMapping("/restaurants")
    public ApiResponse<List<RestaurantDtos.RestaurantResponse>> getRestaurants(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RestaurantDtos.RestaurantResponse> p =
            service.getOwnedRestaurants(user.id(), page, size);
        return ApiResponse.page(p);
    }

    @GetMapping("/restaurants/{id}")
    public ApiResponse<RestaurantDtos.RestaurantResponse> getRestaurant(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return ApiResponse.ok(service.getOwnedRestaurantResponse(user.id(), id));
    }

    @PutMapping("/restaurants/{id}")
    public ApiResponse<RestaurantDtos.RestaurantResponse> updateRestaurant(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.RestaurantRequest request) {
        return ApiResponse.ok(service.updateRestaurant(user.id(), id, request));
    }

    @PostMapping("/restaurants/{id}/menu-items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantDtos.MenuItemResponse> createMenuItem(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @Valid @RequestBody RestaurantDtos.MenuItemRequest request) {
        return ApiResponse.ok(service.createMenuItem(user.id(), id, request));
    }

    @PutMapping("/menu-items/{itemId}")
    public ApiResponse<RestaurantDtos.MenuItemResponse> updateMenuItem(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long itemId,
            @Valid @RequestBody RestaurantDtos.MenuItemRequest request) {
        return ApiResponse.ok(service.updateMenuItem(user.id(), itemId, request));
    }

    @PatchMapping("/menu-items/{itemId}/availability")
    public ApiResponse<RestaurantDtos.MenuItemResponse> updateAvailability(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long itemId,
            @Valid @RequestBody RestaurantDtos.AvailabilityRequest request) {
        return ApiResponse.ok(service.updateAvailability(user.id(), itemId, request.available()));
    }

    @GetMapping("/restaurants/{id}/orders")
    public ApiResponse<List<com.fooddelivery.order.Order>> getRestaurantOrders(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id,
            @RequestParam(defaultValue = "PENDING") OrderStatus status) {
        // Chỉ chủ quán mới được xem
        service.getOwnedRestaurantResponse(user.id(), id);
        return ApiResponse.ok(orderRepository.findByRestaurantIdAndStatus(id, status));
    }

    @PatchMapping("/orders/{orderId}/status")
    public ApiResponse<OrderDtos.OrderResponse> changeOrderStatus(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long orderId,
            @Valid @RequestBody OrderDtos.StatusRequest body) {
        return ApiResponse.ok(orderService.merchantChangeStatus(user.id(), orderId, body.status()));
    }
}
