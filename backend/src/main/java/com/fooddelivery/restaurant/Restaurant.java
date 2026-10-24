package com.fooddelivery.restaurant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
    }

    public void setStatus(RestaurantStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public RestaurantStatus getStatus() { return status; }
    public BigDecimal getAverageRating() { return averageRating; }
    public int getRatingCount() { return ratingCount; }
}
