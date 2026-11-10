package com.fooddelivery.order;

import com.fooddelivery.common.exception.InvalidOrderStateException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false)
    private BigDecimal deliveryFee;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version = 0L;

    protected Order() {}

    public Order(Long customerId, Long restaurantId, Long addressId,
                 BigDecimal subtotal, BigDecimal deliveryFee) {
        this(customerId, restaurantId, addressId, subtotal, deliveryFee,
            BigDecimal.ZERO, null, null);
    }

    public Order(Long customerId, Long restaurantId, Long addressId,
                 BigDecimal subtotal, BigDecimal deliveryFee,
                 BigDecimal discount, String voucherCode, Long voucherId) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.addressId = addressId;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discountAmount = discount == null ? BigDecimal.ZERO : discount;
        this.voucherCode = voucherCode;
        this.voucherId = voucherId;
        BigDecimal total = subtotal.subtract(this.discountAmount).add(deliveryFee);
        this.totalAmount = total.signum() < 0 ? BigDecimal.ZERO : total;
    }

    /** Đổi trạng thái theo đúng quy tắc nghiệp vụ. */
    public void changeStatus(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOrderStateException(status, next);
        }
        this.status = next;
        this.updatedAt = Instant.now();
    }

    public boolean ownedBy(Long userId) { return customerId != null && customerId.equals(userId); }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Long getRestaurantId() { return restaurantId; }
    public Long getAddressId() { return addressId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public String getVoucherCode() { return voucherCode; }
    public Long getVoucherId() { return voucherId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
