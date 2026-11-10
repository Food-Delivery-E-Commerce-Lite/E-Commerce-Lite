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

    @Column(name = "payment_ref", unique = true)
    private String paymentRef;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Payment() {}

    public Payment(Long orderId, Method method) {
        this.orderId = orderId;
        this.method = method;
    }

    public void markPaid() {
        this.status = Status.PAID;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markPaid(String ref) {
        this.status = Status.PAID;
        this.paidAt = Instant.now();
        this.updatedAt = Instant.now();
        if (ref != null && !ref.isBlank()) this.paymentRef = ref;
    }

    public void markFailed(String ref) {
        this.status = Status.FAILED;
        this.updatedAt = Instant.now();
        if (ref != null && !ref.isBlank()) this.paymentRef = ref;
    }

    /** Hoàn tiền đơn ONLINE đã PAID (khi hủy đơn). Idempotent. */
    public void markRefunded() {
        if (this.status == Status.REFUNDED) return;
        this.status = Status.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public void setPaymentRef(String ref) { this.paymentRef = ref; this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Method getMethod() { return method; }
    public Status getStatus() { return status; }
    public Instant getPaidAt() { return paidAt; }
    public String getPaymentRef() { return paymentRef; }
    public Instant getUpdatedAt() { return updatedAt; }
}
