package com.fooddelivery.voucher;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoucherRepository extends JpaRepository<Voucher, Long> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    /** Khóa ghi khi trừ lượt dùng trong placeOrder — chống 2 đơn cùng lúc xài quá quota. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Voucher v where upper(v.code) = upper(:code)")
    Optional<Voucher> findByCodeForUpdate(@org.springframework.data.repository.query.Param("code") String code);
    Page<Voucher> findByRestaurantId(Long restaurantId, Pageable pageable);
    Page<Voucher> findByRestaurantIdIsNull(Pageable pageable);
    List<Voucher> findByActiveTrueAndRestaurantIdIsNull();
}
