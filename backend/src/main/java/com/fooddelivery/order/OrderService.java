package com.fooddelivery.order;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.restaurant.MenuItem;
import com.fooddelivery.restaurant.MenuItemRepository;
import com.fooddelivery.restaurant.Restaurant;
import com.fooddelivery.restaurant.RestaurantRepository;
import com.fooddelivery.restaurant.RestaurantStatus;
import com.fooddelivery.user.AddressRepository;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final BigDecimal baseDeliveryFee;
    private final BigDecimal freeShipThreshold;

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final OrderStatusHistoryRepository history;
    private final RestaurantRepository restaurants;
    private final MenuItemRepository menuItems;
    private final AddressRepository addresses;

    public OrderService(OrderRepository orders, OrderItemRepository orderItems,
                        PaymentRepository payments, OrderStatusHistoryRepository history,
                        RestaurantRepository restaurants, MenuItemRepository menuItems,
                        AddressRepository addresses,
                        @Value("${app.delivery.base-fee:15000}") long baseFee,
                        @Value("${app.delivery.free-threshold:200000}") long freeThreshold) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.payments = payments;
        this.history = history;
        this.restaurants = restaurants;
        this.menuItems = menuItems;
        this.addresses = addresses;
        this.baseDeliveryFee = BigDecimal.valueOf(baseFee);
        this.freeShipThreshold = BigDecimal.valueOf(freeThreshold);
    }

    /** Đặt hàng: giá tính lại từ DB, check giờ mở cửa, phí ship động. */
    @Transactional
    public OrderDtos.OrderResponse placeOrder(Long customerId, OrderDtos.PlaceOrderRequest req) {
        Restaurant restaurant = restaurants.findById(req.restaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", req.restaurantId()));
        if (restaurant.getStatus() != RestaurantStatus.APPROVED) {
            throw new BusinessException("RESTAURANT_NOT_AVAILABLE",
                "Nhà hàng chưa được duyệt hoặc đã bị khóa", HttpStatus.BAD_REQUEST);
        }
        // P1: kiểm tra giờ mở/đóng cửa
        if (!isOpenNow(restaurant)) {
            throw new BusinessException("RESTAURANT_CLOSED",
                "Quán hiện đang đóng cửa, vui lòng quay lại trong giờ mở cửa", HttpStatus.BAD_REQUEST);
        }
        addresses.findByIdAndUserId(req.addressId(), customerId)
            .orElseThrow(() -> new BusinessException("ADDRESS_NOT_FOUND",
                "Địa chỉ giao hàng không hợp lệ", HttpStatus.BAD_REQUEST));

        List<Long> itemIds = req.items().stream().map(OrderDtos.CartLine::itemId).distinct().toList();
        Map<Long, MenuItem> menuMap = menuItems.findAllById(itemIds).stream()
            .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderDtos.CartLine line : req.items()) {
            MenuItem mi = menuMap.get(line.itemId());
            if (mi == null) {
                throw new BusinessException("MENU_ITEM_NOT_FOUND",
                    "Món ăn không tồn tại: " + line.itemId(), HttpStatus.BAD_REQUEST);
            }
            if (!mi.getRestaurantId().equals(req.restaurantId())) {
                throw new BusinessException("MIXED_RESTAURANT",
                    "Giỏ hàng chỉ được chứa món của một nhà hàng", HttpStatus.BAD_REQUEST);
            }
            if (!mi.isAvailable()) {
                throw new BusinessException("ITEM_UNAVAILABLE",
                    "Món '" + mi.getName() + "' hiện không bán", HttpStatus.BAD_REQUEST);
            }
            subtotal = subtotal.add(mi.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        // P1: phí ship động — free ship khi vượt ngưỡng
        BigDecimal deliveryFee = subtotal.compareTo(freeShipThreshold) >= 0
            ? BigDecimal.ZERO : baseDeliveryFee;

        Order order = new Order(customerId, req.restaurantId(), req.addressId(), subtotal, deliveryFee);
        orders.save(order);

        List<OrderItem> lines = new ArrayList<>();
        for (OrderDtos.CartLine line : req.items()) {
            MenuItem mi = menuMap.get(line.itemId());
            lines.add(new OrderItem(order.getId(), mi.getId(), mi.getName(), mi.getPrice(), line.quantity()));
        }
        orderItems.saveAll(lines);
        // P1: ONLINE không còn markPaid ngay — chờ callback cổng thanh toán
        Payment payment = new Payment(order.getId(), req.method());
        payments.save(payment);
        history.save(new OrderStatusHistory(order.getId(), null, OrderStatus.PENDING, customerId));
        return toResponse(order, lines, payment);
    }

    private boolean isOpenNow(Restaurant r) {
        if (r.getOpenTime() == null || r.getCloseTime() == null) return true;
        LocalTime now = LocalTime.now();
        LocalTime open = r.getOpenTime();
        LocalTime close = r.getCloseTime();
        if (open.equals(close)) return true;
        if (open.isBefore(close)) {
            return !now.isBefore(open) && !now.isAfter(close);
        }
        // Quán mở qua đêm (vd 18:00-02:00)
        return !now.isBefore(open) || !now.isAfter(close);
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderResponse> listMyOrders(Long customerId, Pageable pageable) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getMyOrder(Long customerId, Long orderId) {
        Order order = getOrder(orderId);
        if (!order.ownedBy(customerId)) {
            throw new BusinessException("NOT_ORDER_OWNER",
                "Bạn không sở hữu đơn hàng này", HttpStatus.FORBIDDEN);
        }
        return toResponse(order);
    }

    @Transactional
    public OrderDtos.OrderResponse cancelMyOrder(Long customerId, Long orderId) {
        Order order = getOrder(orderId);
        if (!order.ownedBy(customerId)) {
            throw new BusinessException("NOT_ORDER_OWNER",
                "Bạn không sở hữu đơn hàng này", HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("CANNOT_CANCEL",
                "Chỉ được hủy khi đơn ở trạng thái PENDING/CONFIRMED", HttpStatus.CONFLICT);
        }
        OrderStatus from = order.getStatus();
        order.changeStatus(OrderStatus.CANCELLED);
        history.save(new OrderStatusHistory(order.getId(), from, OrderStatus.CANCELLED, customerId));
        return toResponse(order);
    }

    @Transactional
    public OrderDtos.OrderResponse merchantChangeStatus(Long merchantId, Long orderId, OrderStatus next) {
        Order order = getOrder(orderId);
        Restaurant restaurant = restaurants.findById(order.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", order.getRestaurantId()));
        if (!restaurant.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER",
                "Bạn không sở hữu nhà hàng của đơn này", HttpStatus.FORBIDDEN);
        }
        if (next == OrderStatus.CANCELLED
                && order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("CANNOT_CANCEL",
                "Merchant chỉ được hủy đơn PENDING/CONFIRMED", HttpStatus.CONFLICT);
        }
        OrderStatus from = order.getStatus();
        order.changeStatus(next);
        history.save(new OrderStatusHistory(order.getId(), from, next, merchantId));
        if (next == OrderStatus.COMPLETED) {
            payments.findByOrderId(orderId).ifPresent(p -> {
                if (p.getStatus() == Payment.Status.PENDING && p.getMethod() == Payment.Method.COD) p.markPaid();
            });
        }
        return toResponse(order);
    }

    /** Thống kê doanh thu cho merchant: tổng đơn, tổng doanh thu, chia theo trạng thái. */
    @Transactional(readOnly = true)
    public MerchantStats merchantStats(Long merchantId, Long restaurantId) {
        Restaurant r = restaurants.findById(restaurantId)
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (!r.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER", "Bạn không sở hữu nhà hàng này", HttpStatus.FORBIDDEN);
        }
        List<Order> all = orders.findByRestaurantId(restaurantId);
        long total = all.size();
        BigDecimal revenue = all.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending = all.stream().filter(o -> o.getStatus() == OrderStatus.PENDING).count();
        long completed = all.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        return new MerchantStats(total, revenue, pending, completed,
            r.getAverageRating(), r.getRatingCount());
    }

    public record MerchantStats(long totalOrders, BigDecimal revenue, long pending, long completed,
                                BigDecimal avgRating, int ratingCount) {}

    /** Merchant xem đơn của quán mình (đã check chủ quán). */
    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse merchantOrderView(Long merchantId, Long orderId) {
        Order order = getOrder(orderId);
        if (merchantId != null) {
            Restaurant restaurant = restaurants.findById(order.getRestaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", order.getRestaurantId()));
            if (!restaurant.getOwnerId().equals(merchantId)) {
                throw new BusinessException("NOT_OWNER",
                    "Bạn không sở hữu nhà hàng của đơn này", HttpStatus.FORBIDDEN);
            }
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getHistory(Long orderId) {
        return history.findByOrderIdOrderByChangedAtAsc(orderId);
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistory> getHistoryFor(com.fooddelivery.auth.AuthUser user, Long orderId) {
        Order order = getOrder(orderId);
        if (order.ownedBy(user.id())) {
            return getHistory(orderId);
        }
        Restaurant restaurant = restaurants.findById(order.getRestaurantId())
            .orElseThrow(() -> new ResourceNotFoundException("Restaurant", order.getRestaurantId()));
        if (restaurant.getOwnerId().equals(user.id())
                || user.role() == com.fooddelivery.user.Role.ADMIN) {
            return getHistory(orderId);
        }
        throw new BusinessException("NOT_ALLOWED",
            "Bạn không có quyền xem đơn này", HttpStatus.FORBIDDEN);
    }

    private Order getOrder(Long id) {
        return orders.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    private OrderDtos.OrderResponse toResponse(Order order) {
        List<OrderItem> lines = orderItems.findByOrderId(order.getId());
        Payment payment = payments.findByOrderId(order.getId()).orElse(null);
        return toResponse(order, lines, payment);
    }

    private OrderDtos.OrderResponse toResponse(Order order, List<OrderItem> lines, Payment payment) {
        return new OrderDtos.OrderResponse(
            order.getId(), order.getRestaurantId(), order.getAddressId(), order.getStatus(),
            order.getSubtotal(), order.getDeliveryFee(), order.getTotalAmount(),
            payment == null ? null : payment.getMethod(),
            payment == null ? null : payment.getStatus(),
            lines.stream().map(OrderDtos.OrderItemView::from).toList(),
            order.getCreatedAt());
    }
}
