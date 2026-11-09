package com.fooddelivery.user;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import com.fooddelivery.common.exception.BusinessException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {
    private final UserRepository users;
    private final com.fooddelivery.order.OrderRepository orders;
    private final com.fooddelivery.restaurant.RestaurantRepository restaurants;

    public AdminUserController(UserRepository users, com.fooddelivery.order.OrderRepository orders,
                               com.fooddelivery.restaurant.RestaurantRepository restaurants) {
        this.users = users;
        this.orders = orders;
        this.restaurants = restaurants;
    }

    public record UserView(Long id, String email, String fullName, String phone,
                           Role role, boolean active, boolean mustChangePassword, Instant createdAt) {
        static UserView from(User u) {
            return new UserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getRole(), u.isActive(), u.isMustChangePassword(), u.getCreatedAt());
        }
    }

    public record AdminOverview(long totalUsers, long totalMerchants, long totalCustomers,
                                long totalOrders, long pendingRestaurants) {}

    @GetMapping("/users")
    public ApiResponse<List<UserView>> users(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<User> p = role == null
            ? users.findAll(PageRequest.of(page, size))
            : users.findByRole(role, PageRequest.of(page, size));
        return ApiResponse.page(p.map(UserView::from));
    }

    @PatchMapping("/users/{id}/lock")
    @Transactional
    public ApiResponse<UserView> lock(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        if (me.id().equals(id)) {
            throw new BusinessException("CANNOT_LOCK_SELF", "Không thể tự khóa chính mình", HttpStatus.BAD_REQUEST);
        }
        User u = users.findById(id)
            .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Tài khoản không tồn tại", HttpStatus.NOT_FOUND));
        u.deactivate();
        return ApiResponse.ok(UserView.from(users.save(u)));
    }

    @PatchMapping("/users/{id}/unlock")
    @Transactional
    public ApiResponse<UserView> unlock(@PathVariable Long id) {
        User u = users.findById(id)
            .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Tài khoản không tồn tại", HttpStatus.NOT_FOUND));
        u.activate();
        return ApiResponse.ok(UserView.from(users.save(u)));
    }

    @GetMapping("/orders")
    public ApiResponse<List<com.fooddelivery.order.Order>> allOrders(
            @RequestParam(required = false) com.fooddelivery.order.OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<com.fooddelivery.order.Order> p = status == null
            ? orders.findAll(PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending()))
            : orders.findByStatus(status, PageRequest.of(page, size));
        return ApiResponse.page(p);
    }

    @GetMapping("/overview")
    public ApiResponse<AdminOverview> overview() {
        long merchants = users.findByRole(Role.MERCHANT, PageRequest.of(0, 1)).getTotalElements();
        long customers = users.findByRole(Role.CUSTOMER, PageRequest.of(0, 1)).getTotalElements();
        long totalOrders = orders.count();
        long pending = restaurants.findByStatus(
            com.fooddelivery.restaurant.RestaurantStatus.PENDING, PageRequest.of(0, 1)).getTotalElements();
        return ApiResponse.ok(new AdminOverview(merchants + customers + 1, merchants, customers, totalOrders, pending));
    }
}
