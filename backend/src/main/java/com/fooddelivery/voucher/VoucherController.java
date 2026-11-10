package com.fooddelivery.voucher;

import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {
    private final VoucherService vouchers;

    public VoucherController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    /** Khách xem voucher còn dùng được cho quán. */
    @GetMapping("/usable")
    public ApiResponse<List<VoucherDtos.VoucherResponse>> usable(@RequestParam Long restaurantId) {
        return ApiResponse.ok(vouchers.usableFor(restaurantId));
    }

    /** Xem trước giảm giá trước khi đặt (để Checkout tính lại tổng). */
    @PostMapping("/preview")
    public ApiResponse<VoucherDtos.PreviewResponse> preview(
            @Valid @RequestBody VoucherDtos.PreviewRequest req) {
        return ApiResponse.ok(vouchers.preview(req));
    }
}
