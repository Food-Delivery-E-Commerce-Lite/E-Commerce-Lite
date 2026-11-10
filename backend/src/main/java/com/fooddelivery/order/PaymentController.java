package com.fooddelivery.order;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    /**
     * Callback giả lập cổng thanh toán ONLINE (bản Lite).
     * Production: cổng gọi vào đây kèm chữ ký, backend verify rồi mới markPaid.
     * Bản Lite yêu cầu đăng nhập + đúng chủ đơn mới được callback (chống tự markPaid đơn người khác).
     * Idempotent theo paymentRef nên gọi lại nhiều lần vẫn an toàn.
     */
    @PostMapping("/callback")
    public ApiResponse<PaymentService.PaymentView> callback(
            @AuthenticationPrincipal AuthUser me,
            @Valid @RequestBody PaymentService.CallbackRequest req) {
        return ApiResponse.ok(payments.callback(me, req));
    }

    /**
     * P2-3: hoàn tiền thủ công cho đơn ONLINE đã hủy (merchant/admin).
     * Hủy đơn thường đã tự refund; endpoint này để bấm lại khi cần, idempotent.
     */
    @PostMapping("/orders/{orderId}/refund")
    public ApiResponse<PaymentService.PaymentView> refund(
            @AuthenticationPrincipal AuthUser me,
            @PathVariable Long orderId) {
        return ApiResponse.ok(payments.refund(me, orderId));
    }
}
