package com.fooddelivery.voucher;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vouchers")
public class Voucher {
    public enum Type { PERCENT, FIXED, FREESHIP }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type = Type.FIXED;

    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal value = BigDecimal.ZERO;

    @Column(name = "min_order", nullable = false, precision = 12, scale = 0)
    private BigDecimal minOrder = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 12, scale = 0)
    private BigDecimal maxDiscount;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version = 0L;

    protected Voucher() {}

    public Voucher(String code, Type type, BigDecimal value, BigDecimal minOrder,
                   BigDecimal maxDiscount, Integer usageLimit, Long restaurantId,
                   Instant validFrom, Instant validTo) {
        this.code = code.trim().toUpperCase();
        this.type = type;
        this.value = value == null ? BigDecimal.ZERO : value;
        this.minOrder = minOrder == null ? BigDecimal.ZERO : minOrder;
        this.maxDiscount = maxDiscount;
        this.usageLimit = usageLimit;
        this.restaurantId = restaurantId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.active = true;
    }

    public void update(Type type, BigDecimal value, BigDecimal minOrder, BigDecimal maxDiscount,
                       Integer usageLimit, Instant validFrom, Instant validTo, Boolean active) {
        if (type != null) this.type = type;
        if (value != null) this.value = value;
        if (minOrder != null) this.minOrder = minOrder;
        this.maxDiscount = maxDiscount;
        this.usageLimit = usageLimit;
        this.validFrom = validFrom;
        this.validTo = validTo;
        if (active != null) this.active = active;
        this.updatedAt = Instant.now();
    }

    public void consume() { this.usedCount += 1; this.updatedAt = Instant.now(); }

    @PreUpdate
    void touch() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Type getType() { return type; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMinOrder() { return minOrder; }
    public BigDecimal getMaxDiscount() { return maxDiscount; }
    public Integer getUsageLimit() { return usageLimit; }
    public int getUsedCount() { return usedCount; }
    public Long getRestaurantId() { return restaurantId; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidTo() { return validTo; }
    public boolean isActive() { return active; }
}
