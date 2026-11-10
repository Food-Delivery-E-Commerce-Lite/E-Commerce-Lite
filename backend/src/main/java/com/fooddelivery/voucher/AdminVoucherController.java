package com.fooddelivery.voucher;

import com.fooddelivery.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/v1/admin/vouchers")
public class AdminVoucherController {
    private final VoucherService vouchers;

    public AdminVoucherController(VoucherService vouchers) {
        this.vouchers = vouchers;
    }

    @PostMapping
    public ApiResponse<VoucherDtos.VoucherResponse> create(
            @Valid @RequestBody VoucherDtos.CreateRequest req) {
        return ApiResponse.ok(vouchers.createGlobal(req));
    }

    @GetMapping
    public ApiResponse<List<VoucherDtos.VoucherResponse>> list(
            @RequestParam(required = false) Long restaurantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.page(vouchers.listAdmin(restaurantId, page, size));
    }

    @PatchMapping("/{id}")
    public ApiResponse<VoucherDtos.VoucherResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VoucherDtos.UpdateRequest req) {
        return ApiResponse.ok(vouchers.updateAdmin(id, req));
    }
}
