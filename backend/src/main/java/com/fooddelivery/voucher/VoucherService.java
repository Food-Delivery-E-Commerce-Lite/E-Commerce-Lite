package com.fooddelivery.voucher;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.restaurant.RestaurantRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherService {
    private final VoucherRepository vouchers;
    private final RestaurantRepository restaurants;

    public VoucherService(VoucherRepository vouchers, RestaurantRepository restaurants) {
        this.vouchers = vouchers;
        this.restaurants = restaurants;
    }

    @Transactional
    public VoucherDtos.VoucherResponse createGlobal(VoucherDtos.CreateRequest req) {
        return create(req, true);
    }

    @Transactional
    public VoucherDtos.VoucherResponse createForMerchant(Long merchantId, VoucherDtos.CreateRequest req) {
        if (req.restaurantId() == null) {
            throw new BusinessException("RESTAURANT_REQUIRED", "Voucher của quán phải gắn restaurantId", HttpStatus.BAD_REQUEST);
        }
        var r = restaurants.findById(req.restaurantId())
            .orElseThrow(() -> new BusinessException("RESTAURANT_NOT_FOUND", "Quán không tồn tại", HttpStatus.NOT_FOUND));
        if (!r.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER", "Bạn không sở hữu quán này", HttpStatus.FORBIDDEN);
        }
        return create(req, false);
    }

    private VoucherDtos.VoucherResponse create(VoucherDtos.CreateRequest req, boolean allowGlobal) {
        String code = req.code().trim().toUpperCase();
        if (vouchers.findByCodeIgnoreCase(code).isPresent()) {
            throw new BusinessException("VOUCHER_DUP", "Mã voucher đã tồn tại", HttpStatus.CONFLICT);
        }
        if (!allowGlobal && req.restaurantId() == null) {
            throw new BusinessException("RESTAURANT_REQUIRED", "Thiếu restaurantId", HttpStatus.BAD_REQUEST);
        }
        if (req.restaurantId() != null && !restaurants.existsById(req.restaurantId())) {
            throw new BusinessException("RESTAURANT_NOT_FOUND", "Quán không tồn tại", HttpStatus.NOT_FOUND);
        }
        validateValues(req.type(), req.value(), req.minOrder(), req.maxDiscount(), req.usageLimit(), req.validFrom(), req.validTo());
        Voucher v = new Voucher(code, req.type(),
            req.value() == null ? BigDecimal.ZERO : req.value(),
            req.minOrder() == null ? BigDecimal.ZERO : req.minOrder(),
            req.maxDiscount(), req.usageLimit(), req.restaurantId(),
            req.validFrom(), req.validTo());
        return VoucherDtos.VoucherResponse.from(vouchers.save(v));
    }

    @Transactional
    public VoucherDtos.VoucherResponse updateAdmin(Long id, VoucherDtos.UpdateRequest req) {
        Voucher v = get(id);
        validateValues(v.getType(), req.value(), req.minOrder(), req.maxDiscount(), req.usageLimit(), req.validFrom(), req.validTo());
        v.update(req.type(), req.value(), req.minOrder(), req.maxDiscount(), req.usageLimit(),
            req.validFrom(), req.validTo(), req.active());
        return VoucherDtos.VoucherResponse.from(v);
    }

    @Transactional
    public VoucherDtos.VoucherResponse updateForMerchant(Long merchantId, Long id, VoucherDtos.UpdateRequest req) {
        Voucher v = get(id);
        if (v.getRestaurantId() == null) {
            throw new BusinessException("NOT_OWNER", "Voucher toàn sàn chỉ admin sửa", HttpStatus.FORBIDDEN);
        }
        var r = restaurants.findById(v.getRestaurantId())
            .orElseThrow(() -> new BusinessException("RESTAURANT_NOT_FOUND", "Quán không tồn tại", HttpStatus.NOT_FOUND));
        if (!r.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER", "Bạn không sở hữu quán này", HttpStatus.FORBIDDEN);
        }
        v.update(req.type(), req.value(), req.minOrder(), req.maxDiscount(), req.usageLimit(),
            req.validFrom(), req.validTo(), req.active());
        return VoucherDtos.VoucherResponse.from(v);
    }

    @Transactional(readOnly = true)
    public Page<VoucherDtos.VoucherResponse> listAdmin(Long restaurantId, int page, int size) {
        var paging = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        if (restaurantId != null) return vouchers.findByRestaurantId(restaurantId, paging).map(VoucherDtos.VoucherResponse::from);
        return vouchers.findAll(paging).map(VoucherDtos.VoucherResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<VoucherDtos.VoucherResponse> listMerchant(Long merchantId, Long restaurantId, int page, int size) {
        var r = restaurants.findById(restaurantId)
            .orElseThrow(() -> new BusinessException("RESTAURANT_NOT_FOUND", "Quán không tồn tại", HttpStatus.NOT_FOUND));
        if (!r.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER", "Bạn không sở hữu quán này", HttpStatus.FORBIDDEN);
        }
        return vouchers.findByRestaurantId(restaurantId,
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100))).map(VoucherDtos.VoucherResponse::from);
    }

    /** Khách xem các voucher còn dùng được cho quán (lọc active + còn hạn + còn lượt, chưa check min_order). */
    @Transactional(readOnly = true)
    public java.util.List<VoucherDtos.VoucherResponse> usableFor(Long restaurantId) {
        java.util.List<Voucher> all = new java.util.ArrayList<>(vouchers.findByActiveTrueAndRestaurantIdIsNull());
        vouchers.findByRestaurantId(restaurantId, PageRequest.of(0, 100)).forEach(all::add);
        Instant now = Instant.now();
        return all.stream()
            .filter(v -> v.isActive() && withinTime(v, now) && hasQuota(v))
            .map(VoucherDtos.VoucherResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VoucherDtos.PreviewResponse preview(VoucherDtos.PreviewRequest req) {
        var v = vouchers.findByCodeIgnoreCase(req.code().trim().toUpperCase()).orElse(null);
        if (v == null) {
            return new VoucherDtos.PreviewResponse(req.code(), false, "Mã không tồn tại",
                BigDecimal.ZERO, req.deliveryFee(), req.subtotal().add(req.deliveryFee()));
        }
        String err = checkUsable(v, req.restaurantId(), req.subtotal());
        if (err != null) {
            return new VoucherDtos.PreviewResponse(v.getCode(), false, err,
                BigDecimal.ZERO, req.deliveryFee(), req.subtotal().add(req.deliveryFee()));
        }
        var calc = calc(v, req.subtotal(), req.deliveryFee());
        return new VoucherDtos.PreviewResponse(v.getCode(), true, "Áp dụng được",
            calc.discount(), calc.deliveryAfter(), calc.totalAfter());
    }

    /** Dùng trong placeOrder: validate + tính + consume (tăng used_count). Gọi trong transaction đặt hàng. */
    public Applied applyForOrder(String code, Long restaurantId, BigDecimal subtotal, BigDecimal deliveryFee) {
        if (code == null || code.isBlank()) return new Applied(null, null, BigDecimal.ZERO, deliveryFee);
        // Khóa ghi chống 2 đơn cùng lúc xài quá quota (PESSIMISTIC_WRITE)
        var v = vouchers.findByCodeForUpdate(code.trim().toUpperCase())
            .orElseThrow(() -> new BusinessException("VOUCHER_NOT_FOUND", "Mã giảm giá không tồn tại", HttpStatus.BAD_REQUEST));
        String err = checkUsable(v, restaurantId, subtotal);
        if (err != null) {
            throw new BusinessException("VOUCHER_INVALID", err, HttpStatus.BAD_REQUEST);
        }
        var calc = calc(v, subtotal, deliveryFee);
        v.consume();
        return new Applied(v.getId(), v.getCode(), calc.discount(), calc.deliveryAfter());
    }

    public record Applied(Long voucherId, String voucherCode, BigDecimal discount, BigDecimal deliveryAfter) {}
    private record Calc(BigDecimal discount, BigDecimal deliveryAfter, BigDecimal totalAfter) {}

    private Calc calc(Voucher v, BigDecimal subtotal, BigDecimal deliveryFee) {
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal feeAfter = deliveryFee;
        switch (v.getType()) {
            case FREESHIP -> feeAfter = BigDecimal.ZERO;
            case FIXED -> discount = v.getValue().min(subtotal);
            case PERCENT -> {
                discount = subtotal.multiply(v.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
                if (v.getMaxDiscount() != null) discount = discount.min(v.getMaxDiscount());
                discount = discount.min(subtotal);
            }
        }
        BigDecimal total = subtotal.subtract(discount).add(feeAfter);
        if (total.signum() < 0) total = BigDecimal.ZERO;
        return new Calc(discount, feeAfter, total);
    }

    private String checkUsable(Voucher v, Long restaurantId, BigDecimal subtotal) {
        if (!v.isActive()) return "Voucher đã tắt";
        Instant now = Instant.now();
        if (!withinTime(v, now)) return "Voucher hết hạn hoặc chưa tới ngày dùng";
        if (!hasQuota(v)) return "Voucher đã hết lượt dùng";
        if (v.getRestaurantId() != null && !v.getRestaurantId().equals(restaurantId)) return "Voucher không áp dụng cho quán này";
        if (subtotal.compareTo(v.getMinOrder()) < 0) return "Đơn tối thiểu " + v.getMinOrder() + "đ mới dùng được";
        return null;
    }

    private boolean withinTime(Voucher v, Instant now) {
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom())) return false;
        if (v.getValidTo() != null && now.isAfter(v.getValidTo())) return false;
        return true;
    }

    private boolean hasQuota(Voucher v) {
        return v.getUsageLimit() == null || v.getUsedCount() < v.getUsageLimit();
    }

    private void validateValues(Voucher.Type type, BigDecimal value, BigDecimal minOrder,
                                BigDecimal maxDiscount, Integer usageLimit, Instant from, Instant to) {
        if (type == Voucher.Type.PERCENT && value != null) {
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessException("VOUCHER_VALUE", "PERCENT phải 1-100", HttpStatus.BAD_REQUEST);
            }
        }
        if (type == Voucher.Type.FIXED && value != null && value.signum() < 0) {
            throw new BusinessException("VOUCHER_VALUE", "Giá trị giảm phải >= 0", HttpStatus.BAD_REQUEST);
        }
        if (usageLimit != null && usageLimit <= 0) {
            throw new BusinessException("VOUCHER_LIMIT", "usageLimit phải > 0", HttpStatus.BAD_REQUEST);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("VOUCHER_TIME", "validFrom phải trước validTo", HttpStatus.BAD_REQUEST);
        }
    }

    private Voucher get(Long id) {
        return vouchers.findById(id)
            .orElseThrow(() -> new BusinessException("VOUCHER_NOT_FOUND", "Voucher không tồn tại", HttpStatus.NOT_FOUND));
    }
}
