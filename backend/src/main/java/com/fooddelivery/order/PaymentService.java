package com.fooddelivery.order;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.realtime.OrderEvents;
import com.fooddelivery.restaurant.RestaurantRepository;
import com.fooddelivery.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final RestaurantRepository restaurants;
    private final OrderEvents events;

    public PaymentService(PaymentRepository payments, OrderRepository orders,
                          RestaurantRepository restaurants,
                          OrderEvents events) {
        this.payments = payments;
        this.orders = orders;
        this.restaurants = restaurants;
        this.events = events;
    }

    public record CallbackRequest(
        @NotNull Long orderId,
        @NotBlank String paymentRef,
        boolean success) {}

    public record PaymentView(Long orderId, Payment.Method method, Payment.Status status, String paymentRef) {
        static PaymentView from(Payment p) {
            return new PaymentView(p.getOrderId(), p.getMethod(), p.getStatus(), p.getPaymentRef());
        }
    }

    /**
     * Callback thanh toán ONLINE, idempotent theo paymentRef:
     * - Gọi lại cùng paymentRef nhiều lần chỉ có hiệu lực 1 lần.
     * - paymentRef mới cho đơn đã PAID thì bỏ qua (tránh double-charge).
     * - Bản Lite: chỉ chủ đơn / chủ quán / ADMIN được gọi (chống tự markPaid đơn người khác).
     */
    @Transactional
    public PaymentView callback(AuthUser me, CallbackRequest req) {
        var order = orders.findById(req.orderId())
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                "Đơn hàng không tồn tại", HttpStatus.NOT_FOUND));
        // Chặn gọi callback cho đơn của người khác
        if (me != null) {
            boolean isOwner = order.ownedBy(me.id());
            boolean isAdmin = me.role() == Role.ADMIN;
            if (!isOwner && !isAdmin) {
                var r = restaurants.findById(order.getRestaurantId()).orElse(null);
                boolean isMerchantOwner = r != null && r.getOwnerId() != null && r.getOwnerId().equals(me.id());
                if (!isMerchantOwner) {
                    throw new BusinessException("NOT_ALLOWED",
                        "Bạn không có quyền cập nhật thanh toán đơn này", HttpStatus.FORBIDDEN);
                }
            }
        }
        Payment payment = payments.findByOrderId(order.getId())
            .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                "Chưa có thông tin thanh toán", HttpStatus.NOT_FOUND));
        if (payment.getMethod() != Payment.Method.ONLINE) {
            throw new BusinessException("NOT_ONLINE_PAYMENT",
                "Đơn COD không cần callback online", HttpStatus.BAD_REQUEST);
        }
        // Idempotent: trùng ref thì trả về luôn
        if (req.paymentRef().equals(payment.getPaymentRef())) {
            return PaymentView.from(payment);
        }
        // Đơn đã PAID rồi mà ref khác tới -> bỏ qua, không đổi trạng thái (chống thu tiền 2 lần)
        if (payment.getStatus() == Payment.Status.PAID) {
            return PaymentView.from(payment);
        }
        // Ref đã dùng cho đơn khác -> xung đột
        var dup = payments.findByPaymentRef(req.paymentRef());
        if (dup.isPresent() && !dup.get().getOrderId().equals(order.getId())) {
            throw new BusinessException("PAYMENT_REF_DUP",
                "Mã giao dịch đã được dùng", HttpStatus.CONFLICT);
        }
        if (req.success()) {
            payment.markPaid(req.paymentRef());
        } else {
            payment.markFailed(req.paymentRef());
        }
        events.paymentChanged(order.getId(), order.getRestaurantId(), payment.getStatus().name());
        return PaymentView.from(payment);
    }

    /**
     * P2-3: hoàn tiền thủ công cho đơn ONLINE đã PAID (idempotent).
     * Thường không cần gọi tay vì hủy đơn đã tự refund, endpoint này để
     * merchant/admin bấm lại khi callback refund trước đó lỗi.
     * Quyền: chủ đơn, chủ quán của đơn, hoặc ADMIN.
     */
    @Transactional
    public PaymentView refund(AuthUser me, Long orderId) {
        var order = orders.findById(orderId)
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                "Đơn hàng không tồn tại", HttpStatus.NOT_FOUND));
        boolean isOwner = order.ownedBy(me.id());
        boolean isAdmin = me.role() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            var r = restaurants.findById(order.getRestaurantId()).orElse(null);
            boolean isMerchantOwner = r != null && r.getOwnerId() != null && r.getOwnerId().equals(me.id());
            if (!isMerchantOwner) {
                throw new BusinessException("NOT_ALLOWED",
                    "Bạn không có quyền hoàn tiền đơn này", HttpStatus.FORBIDDEN);
            }
        }
        Payment payment = payments.findByOrderId(order.getId())
            .orElseThrow(() -> new BusinessException("PAYMENT_NOT_FOUND",
                "Chưa có thông tin thanh toán", HttpStatus.NOT_FOUND));
        if (payment.getMethod() != Payment.Method.ONLINE) {
            throw new BusinessException("NOT_ONLINE_PAYMENT",
                "Đơn COD không cần hoàn tiền online", HttpStatus.BAD_REQUEST);
        }
        if (payment.getStatus() == Payment.Status.REFUNDED) {
            return PaymentView.from(payment);
        }
        if (payment.getStatus() != Payment.Status.PAID) {
            throw new BusinessException("NOT_PAID",
                "Chỉ hoàn tiền đơn đã thanh toán", HttpStatus.BAD_REQUEST);
        }
        if (order.getStatus() != OrderStatus.CANCELLED) {
            throw new BusinessException("NOT_CANCELLED",
                "Chỉ hoàn tiền đơn đã hủy", HttpStatus.BAD_REQUEST);
        }
        payment.markRefunded();
        events.paymentChanged(order.getId(), order.getRestaurantId(), payment.getStatus().name());
        return PaymentView.from(payment);
    }
}
