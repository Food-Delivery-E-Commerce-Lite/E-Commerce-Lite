package com.fooddelivery.report;

import java.math.BigDecimal;
import java.time.LocalDate;

public final class ReportDtos {
    private ReportDtos() {}

    /** 1 điểm trên biểu đồ doanh thu theo ngày. */
    public record DailyPoint(LocalDate date, long orders, BigDecimal revenue, BigDecimal discount) {}

    public record StatusSlice(String status, long orders, BigDecimal revenue) {}

    public record PaySlice(String method, String status, long count) {}

    public record TopDish(Long itemId, String itemName, long quantity, BigDecimal revenue) {}

    public record TopRestaurant(Long restaurantId, String name, long orders, BigDecimal revenue, double avgRating, int ratingCount) {}

    /** Báo cáo chi tiết 1 quán: tổng quan + theo ngày + trạng thái + món bán chạy. */
    public record MerchantReport(
        Long restaurantId,
        LocalDate from, LocalDate to,
        long totalOrders, long completedOrders, long cancelledOrders,
        BigDecimal revenue, BigDecimal discount, BigDecimal avgOrderValue,
        double cancelRate,
        java.util.List<DailyPoint> daily,
        java.util.List<StatusSlice> byStatus,
        java.util.List<TopDish> topDishes) {}

    /** Báo cáo toàn sàn cho admin. */
    public record AdminReport(
        LocalDate from, LocalDate to,
        long totalOrders, long completedOrders, long cancelledOrders,
        BigDecimal revenue, BigDecimal discount, BigDecimal avgOrderValue,
        double cancelRate,
        long totalCustomers, long totalMerchants,
        java.util.List<DailyPoint> daily,
        java.util.List<StatusSlice> byStatus,
        java.util.List<PaySlice> byPayment,
        java.util.List<TopRestaurant> topRestaurants,
        java.util.List<TopDish> topDishes) {}
}
