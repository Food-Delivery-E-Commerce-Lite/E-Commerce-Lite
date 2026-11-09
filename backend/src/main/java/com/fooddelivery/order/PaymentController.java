package com.fooddelivery.order;

import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
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
     * Idempotent theo paymentRef nên gọi lại nhiều lần vẫn an toàn.
     */
    @PostMapping("/callback")
    public ApiResponse<PaymentService.PaymentView> callback(
            @Valid @RequestBody PaymentService.CallbackRequest req) {
        return ApiResponse.ok(payments.callback(req));
    }
}
