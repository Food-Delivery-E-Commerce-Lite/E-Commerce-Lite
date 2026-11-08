package com.fooddelivery.order;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    public enum Method { COD, ONLINE }
    public enum Status { PENDING, PAID, FAILED, REFUNDED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method = Method.COD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "paid_at")
    private Instant paidAt;

    protected Payment() {}

    public Payment(Long orderId, Method method) {
        this.orderId = orderId;
        this.method = method;
    }

    public void markPaid() {
        this.status = Status.PAID;
        this.paidAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Method getMethod() { return method; }
    public Status getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
}
