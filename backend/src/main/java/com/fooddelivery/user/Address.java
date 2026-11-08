package com.fooddelivery.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private String label;

    @Column(nullable = false)
    private String street;

    private String district;
    private String city;
    private java.math.BigDecimal lat;
    private java.math.BigDecimal lng;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Address() {}

    public Address(Long userId, String label, String street, String district, String city, boolean isDefault) {
        this.userId = userId;
        this.label = label;
        this.street = street;
        this.district = district;
        this.city = city;
        this.isDefault = isDefault;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getLabel() { return label; }
    public String getStreet() { return street; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean v) { this.isDefault = v; }
}
