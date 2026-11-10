package com.fooddelivery.voucher;

import com.fooddelivery.auth.AuthUser;
import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/merchant/vouchers")
public class MerchantVoucherController {
    private final VoucherService vouchers;

    public MerchantVoucherController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    @PostMapping
    public ApiResponse<VoucherDtos.VoucherResponse> create(
            @AuthenticationPrincipal AuthUser me,
            @Valid @RequestBody VoucherDtos.CreateRequest req) {
        return ApiResponse.ok(vouchers.createForMerchant(me.id(), req));
    }

    @GetMapping
    public ApiResponse<List<VoucherDtos.VoucherResponse>> list(
            @AuthenticationPrincipal AuthUser me,
            @RequestParam Long restaurantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(vouchers.listMerchant(me.id(), restaurantId, page, size));
    }

    @PatchMapping("/{id}")
    public ApiResponse<VoucherDtos.VoucherResponse> update(
            @AuthenticationPrincipal AuthUser me, @PathVariable Long id,
            @Valid @RequestBody VoucherDtos.UpdateRequest req) {
        return ApiResponse.ok(vouchers.updateForMerchant(me.id(), id, req));
    }
}
