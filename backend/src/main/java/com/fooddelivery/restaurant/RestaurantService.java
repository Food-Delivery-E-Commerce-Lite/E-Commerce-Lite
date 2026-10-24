package com.fooddelivery.restaurant;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurants;
    private final CategoryRepository categories;
    private final MenuItemRepository menuItems;

    public RestaurantService(RestaurantRepository restaurants, CategoryRepository categories,
                             MenuItemRepository menuItems) {
        this.restaurants = restaurants;
        this.categories = categories;
        this.menuItems = menuItems;
    }

    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> search(String keyword, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        PageRequest paging = PageRequest.of(page, size);
        Page<Restaurant> results = normalizedKeyword == null
            ? restaurants.findByStatus(RestaurantStatus.APPROVED, paging)
            : restaurants.findByStatusAndNameContainingIgnoreCase(
                RestaurantStatus.APPROVED, normalizedKeyword, paging);
        return results.map(RestaurantDtos.RestaurantResponse::from);
    }

    @Transactional(readOnly = true)
    public RestaurantDtos.RestaurantResponse getPublicRestaurant(Long id) {
        Restaurant restaurant = getRestaurant(id);
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ResourceNotFoundException("Restaurant", id);
        }
        return RestaurantDtos.RestaurantResponse.from(restaurant);
    }

    @Transactional(readOnly = true)
    public List<RestaurantDtos.MenuItemResponse> getPublicMenu(Long id) {
        Restaurant restaurant = getRestaurant(id);
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ResourceNotFoundException("Restaurant", id);
        }
        return menuItems.findByRestaurantIdAndAvailableTrue(id).stream()
            .map(RestaurantDtos.MenuItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RestaurantDtos.CategoryResponse> getCategories() {
        return categories.findAll().stream()
            .map(RestaurantDtos.CategoryResponse::from).toList();
    }

    @Transactional
    public RestaurantDtos.RestaurantResponse createRestaurant(
            Long ownerId, RestaurantDtos.RestaurantRequest request) {
        Restaurant restaurant = new Restaurant(ownerId, request.name().trim(),
            request.description(), request.address().trim(), request.openTime(), request.closeTime());
        return RestaurantDtos.RestaurantResponse.from(restaurants.save(restaurant));
    }

    @Transactional(readOnly = true)
    public List<RestaurantDtos.RestaurantResponse> getOwnedRestaurants(Long ownerId) {
        return restaurants.findByOwnerId(ownerId).stream()
            .map(RestaurantDtos.RestaurantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public RestaurantDtos.RestaurantResponse getOwnedRestaurantResponse(Long ownerId, Long restaurantId) {
        return RestaurantDtos.RestaurantResponse.from(getOwnedRestaurant(ownerId, restaurantId));
    }

    @Transactional
    public RestaurantDtos.RestaurantResponse updateRestaurant(
            Long ownerId, Long restaurantId, RestaurantDtos.RestaurantRequest request) {
        Restaurant restaurant = getOwnedRestaurant(ownerId, restaurantId);
        restaurant.update(request.name().trim(), request.description(), request.address().trim(),
            request.openTime(), request.closeTime());
        return RestaurantDtos.RestaurantResponse.from(restaurant);
    }

    @Transactional
    public RestaurantDtos.MenuItemResponse createMenuItem(
            Long ownerId, Long restaurantId, RestaurantDtos.MenuItemRequest request) {
        getOwnedRestaurant(ownerId, restaurantId);
        validateCategory(request.categoryId());
        MenuItem item = new MenuItem(restaurantId, request.categoryId(), request.name().trim(),
            request.description(), request.price(), request.imageUrl());
        return RestaurantDtos.MenuItemResponse.from(menuItems.save(item));
    }

    @Transactional
    public RestaurantDtos.MenuItemResponse updateMenuItem(
            Long ownerId, Long itemId, RestaurantDtos.MenuItemRequest request) {
        MenuItem item = getMenuItem(itemId);
        getOwnedRestaurant(ownerId, item.getRestaurantId());
        validateCategory(request.categoryId());
        item.update(request.categoryId(), request.name().trim(), request.description(),
            request.price(), request.imageUrl());
        return RestaurantDtos.MenuItemResponse.from(item);
    }

    @Transactional
    public RestaurantDtos.MenuItemResponse updateAvailability(
            Long ownerId, Long itemId, boolean available) {
        MenuItem item = getMenuItem(itemId);
        getOwnedRestaurant(ownerId, item.getRestaurantId());
        item.setAvailable(available);
        return RestaurantDtos.MenuItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> getRestaurantsByStatus(
            RestaurantStatus status, int page, int size) {
        return restaurants.findByStatus(status, PageRequest.of(page, size))
            .map(RestaurantDtos.RestaurantResponse::from);
    }

    @Transactional
    public RestaurantDtos.RestaurantResponse updateStatus(
            Long restaurantId, RestaurantStatus status) {
        if (status == RestaurantStatus.PENDING) {
            throw new BusinessException("INVALID_RESTAURANT_STATUS",
                "Trạng thái mới phải là APPROVED, REJECTED hoặc LOCKED", HttpStatus.BAD_REQUEST);
        }
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setStatus(status);
        return RestaurantDtos.RestaurantResponse.from(restaurant);
    }

    private Restaurant getRestaurant(Long id) {
        return restaurants.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", id));
    }

    private Restaurant getOwnedRestaurant(Long ownerId, Long restaurantId) {
        Restaurant restaurant = getRestaurant(restaurantId);
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new BusinessException("NOT_OWNER",
                "Bạn không sở hữu nhà hàng này", HttpStatus.FORBIDDEN);
        }
        return restaurant;
    }

    private MenuItem getMenuItem(Long id) {
        return menuItems.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null && !categories.existsById(categoryId)) {
            throw new BusinessException("CATEGORY_NOT_FOUND",
                "Không tìm thấy danh mục " + categoryId, HttpStatus.NOT_FOUND);
        }
    }
}
