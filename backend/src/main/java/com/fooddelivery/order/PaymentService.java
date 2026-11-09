package com.fooddelivery.order;

import com.fooddelivery.common.exception.BusinessException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {
    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;

    public PaymentService(PaymentRepository payments, OrderRepository orders,
                          OrderStatusHistoryRepository history) {
        this.payments = payments;
        this.orders = orders;
        this.history = history;
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
     */
    @Transactional
    public PaymentView callback(CallbackRequest req) {
        var order = orders.findById(req.orderId())
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                "Đơn hàng không tồn tại", HttpStatus.NOT_FOUND));
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
        return PaymentView.from(payment);
    }
}
