package com.fooddelivery.restaurant;

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
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "restaurants")
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String address;

    private BigDecimal lat;
    private BigDecimal lng;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestaurantStatus status = RestaurantStatus.PENDING;

    @Column(name = "avg_rating", nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    private int ratingCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version = 0L;

    protected Restaurant() {}

    public Restaurant(Long ownerId, String name, String description, String address,
                      LocalTime openTime, LocalTime closeTime) {
        this.ownerId = ownerId;
        update(name, description, address, openTime, closeTime);
        this.status = RestaurantStatus.PENDING;
    }

    public void update(String name, String description, String address,
                       LocalTime openTime, LocalTime closeTime) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.updatedAt = Instant.now();
    }

    /** Merchant sửa quán phải duyệt lại: quay về PENDING. */
    public void updateAndRequestReapproval(String name, String description, String address,
                                          LocalTime openTime, LocalTime closeTime) {
        update(name, description, address, openTime, closeTime);
        if (this.status == RestaurantStatus.APPROVED) {
            this.status = RestaurantStatus.PENDING;
        }
    }

    public void setStatus(RestaurantStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    /** Cập nhật rating trung bình nguyên tử trong transaction (gọi kèm lock lạc quan). */
    public void addRating(int rating) {
        BigDecimal total = averageRating.multiply(BigDecimal.valueOf(ratingCount))
            .add(BigDecimal.valueOf(rating));
        ratingCount += 1;
        averageRating = total.divide(BigDecimal.valueOf(ratingCount), 2, RoundingMode.HALF_UP);
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public BigDecimal getLat() { return lat; }
    public BigDecimal getLng() { return lng; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public RestaurantStatus getStatus() { return status; }
    public BigDecimal getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
    public Instant getUpdatedAt() { return updatedAt; }
}
