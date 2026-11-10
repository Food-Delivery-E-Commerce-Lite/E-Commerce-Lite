package com.fooddelivery.restaurant;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
    private final RestaurantRepository restaurants;
    private final CategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final ReviewRepository reviews;

    public RestaurantService(RestaurantRepository restaurants, CategoryRepository categories,
                             MenuItemRepository menuItems, ReviewRepository reviews) {
        this.restaurants = restaurants;
        this.categories = categories;
        this.menuItems = menuItems;
        this.reviews = reviews;
    }

    /** Tìm kiếm nâng cao: keyword + category + rating tối thiểu + sắp xếp. */
    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> search(String keyword, Long categoryId,
                                                          BigDecimal minRating, String sort,
                                                          int page, int size) {
        PageRequest paging = PageRequest.of(clampPage(page), clampSize(size), resolveSort(sort));
        Specification<Restaurant> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), RestaurantStatus.APPROVED));
            if (keyword != null && !keyword.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%"));
            }
            if (minRating != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }
            if (categoryId != null) {
                var sub = q.subquery(Long.class);
                var mi = sub.from(MenuItem.class);
                sub.select(mi.get("restaurantId"))
                   .where(cb.and(
                       cb.equal(mi.get("restaurantId"), root.get("id")),
                       cb.equal(mi.get("categoryId"), categoryId),
                       cb.isTrue(mi.get("available"))));
                ps.add(cb.exists(sub));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return restaurants.search(spec, paging).map(RestaurantDtos.RestaurantResponse::from);
    }

    // Giữ tương thích API cũ
    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> search(String keyword, int page, int size) {
        return search(keyword, null, null, "newest", page, size);
    }

    private Sort resolveSort(String sort) {
        if ("rating".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.DESC, "averageRating").and(Sort.by(Sort.Direction.DESC, "id"));
        }
        if ("name".equalsIgnoreCase(sort)) {
            return Sort.by(Sort.Direction.ASC, "name");
        }
        return Sort.by(Sort.Direction.DESC, "id");
    }

    @Transactional(readOnly = true)
    public RestaurantDtos.RestaurantResponse getPublicRestaurant(Long id) {
        return RestaurantDtos.RestaurantResponse.from(requireApproved(id));
    }

    @Transactional(readOnly = true)
    public List<RestaurantDtos.MenuItemResponse> getPublicMenu(Long id) {
        requireApproved(id);
        return menuItems.findByRestaurantId(id).stream()
            .filter(MenuItem::isAvailable)
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
        restaurant.setImageUrl(normalizeUrl(request.imageUrl()));
        return RestaurantDtos.RestaurantResponse.from(restaurants.save(restaurant));
    }

    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> getOwnedRestaurants(Long ownerId, int page, int size) {
        return restaurants.findByOwnerId(ownerId, PageRequest.of(clampPage(page), clampSize(size)))
            .map(RestaurantDtos.RestaurantResponse::from);
    }

    @Transactional(readOnly = true)
    public RestaurantDtos.RestaurantResponse getOwnedRestaurantResponse(Long ownerId, Long restaurantId) {
        return RestaurantDtos.RestaurantResponse.from(getOwnedRestaurant(ownerId, restaurantId));
    }

    @Transactional
    public RestaurantDtos.RestaurantResponse updateRestaurant(
            Long ownerId, Long restaurantId, RestaurantDtos.RestaurantRequest request) {
        Restaurant restaurant = getOwnedRestaurant(ownerId, restaurantId);
        // Sửa quán đã duyệt phải duyệt lại -> chống lách kiểm duyệt
        restaurant.updateAndRequestReapproval(request.name().trim(), request.description(),
            request.address().trim(), request.openTime(), request.closeTime());
        restaurant.setImageUrl(normalizeUrl(request.imageUrl()));
        return RestaurantDtos.RestaurantResponse.from(restaurant);
    }

    @Transactional
    public RestaurantDtos.MenuItemResponse createMenuItem(
            Long ownerId, Long restaurantId, RestaurantDtos.MenuItemRequest request) {
        getOwnedRestaurant(ownerId, restaurantId);
        validateCategory(request.categoryId());
        MenuItem item = new MenuItem(restaurantId, request.categoryId(), request.name().trim(),
            request.description(), request.price(), normalizeUrl(request.imageUrl()));
        return RestaurantDtos.MenuItemResponse.from(menuItems.save(item));
    }

    @Transactional
    public RestaurantDtos.MenuItemResponse updateMenuItem(
            Long ownerId, Long itemId, RestaurantDtos.MenuItemRequest request) {
        MenuItem item = getMenuItem(itemId);
        getOwnedRestaurant(ownerId, item.getRestaurantId());
        validateCategory(request.categoryId());
        item.update(request.categoryId(), request.name().trim(), request.description(),
            request.price(), normalizeUrl(request.imageUrl()));
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

    @Transactional
    public void deleteMenuItem(Long ownerId, Long itemId) {
        MenuItem item = getMenuItem(itemId);
        getOwnedRestaurant(ownerId, item.getRestaurantId());
        menuItems.delete(item);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<RestaurantDtos.MenuItemResponse> getOwnedMenu(
            Long ownerId, Long restaurantId, int page, int size) {
        getOwnedRestaurant(ownerId, restaurantId);
        return menuItems.findByRestaurantId(restaurantId,
                PageRequest.of(clampPage(page), clampSize(size)))
            .map(RestaurantDtos.MenuItemResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RestaurantDtos.RestaurantResponse> getRestaurantsByStatus(
            RestaurantStatus status, int page, int size) {
        return restaurants.findByStatus(status, PageRequest.of(clampPage(page), clampSize(size)))
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

    private Restaurant requireApproved(Long id) {
        Restaurant restaurant = getRestaurant(id);
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new ResourceNotFoundException("Restaurant", id);
        }
        return restaurant;
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

    private int clampPage(int page) { return Math.max(page, 0); }
    private int clampSize(int size) { return Math.min(Math.max(size, 1), 100); }

    /** Chuẩn hóa URL ảnh: cho phép /uploads/... hoặc http(s), chặn javascript:/file: */
    private String normalizeUrl(String url) {
        if (url == null) return null;
        String v = url.trim();
        if (v.isEmpty()) return null;
        if (v.startsWith("/uploads/")) return v;
        if (v.startsWith("http://") || v.startsWith("https://")) return v;
        throw new BusinessException("INVALID_IMAGE_URL",
            "Link ảnh phải là /uploads/... hoặc http(s)", HttpStatus.BAD_REQUEST);
    }
}
