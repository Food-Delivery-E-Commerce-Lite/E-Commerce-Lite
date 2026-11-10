package com.fooddelivery.voucher;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public final class VoucherDtos {
    private VoucherDtos() {}

    public record CreateRequest(
        @NotBlank @Size(max = 50) String code,
        @NotNull Voucher.Type type,
        @DecimalMin("0") BigDecimal value,
        @DecimalMin("0") BigDecimal minOrder,
        @DecimalMin("0") BigDecimal maxDiscount,
        Integer usageLimit,
        Long restaurantId,
        Instant validFrom,
        Instant validTo) {}

    public record UpdateRequest(
        Voucher.Type type,
        @DecimalMin("0") BigDecimal value,
        @DecimalMin("0") BigDecimal minOrder,
        @DecimalMin("0") BigDecimal maxDiscount,
        Integer usageLimit,
        Instant validFrom,
        Instant validTo,
        Boolean active) {}

    public record VoucherResponse(
        Long id, String code, Voucher.Type type, BigDecimal value,
        BigDecimal minOrder, BigDecimal maxDiscount, Integer usageLimit,
        int usedCount, Long restaurantId, Instant validFrom, Instant validTo,
        boolean active) {
        static VoucherResponse from(Voucher v) {
            return new VoucherResponse(v.getId(), v.getCode(), v.getType(), v.getValue(),
                v.getMinOrder(), v.getMaxDiscount(), v.getUsageLimit(), v.getUsedCount(),
                v.getRestaurantId(), v.getValidFrom(), v.getValidTo(), v.isActive());
        }
    }

    public record PreviewRequest(
        @NotBlank String code,
        @NotNull Long restaurantId,
        @NotNull @DecimalMin("0") BigDecimal subtotal,
        @NotNull @DecimalMin("0") BigDecimal deliveryFee) {}

    public record PreviewResponse(
        String code, boolean usable, String message,
        BigDecimal discount, BigDecimal deliveryFeeAfter, BigDecimal totalAfter) {}
}
