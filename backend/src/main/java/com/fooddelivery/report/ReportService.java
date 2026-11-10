package com.fooddelivery.report;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.Order;
import com.fooddelivery.order.OrderItem;
import com.fooddelivery.order.OrderItemRepository;
import com.fooddelivery.order.OrderRepository;
import com.fooddelivery.order.OrderStatus;
import com.fooddelivery.order.Payment;
import com.fooddelivery.order.PaymentRepository;
import com.fooddelivery.restaurant.Restaurant;
import com.fooddelivery.restaurant.RestaurantRepository;
import com.fooddelivery.user.Role;
import com.fooddelivery.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final PaymentRepository payments;
    private final RestaurantRepository restaurants;
    private final UserRepository users;

    public ReportService(OrderRepository orders, OrderItemRepository orderItems,
                         PaymentRepository payments, RestaurantRepository restaurants,
                         UserRepository users) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.payments = payments;
        this.restaurants = restaurants;
        this.users = users;
    }

    public record Range(Instant fromInc, Instant toExc, LocalDate from, LocalDate to) {}

    public Range range(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(ZONE);
        LocalDate start = from != null ? from : end.minusDays(29);
        if (start.isAfter(end)) {
            throw new BusinessException("BAD_RANGE", "from phải trước to", HttpStatus.BAD_REQUEST);
        }
        if (end.toEpochDay() - start.toEpochDay() > 366) {
            throw new BusinessException("RANGE_TOO_WIDE", "Chỉ xem tối đa 366 ngày", HttpStatus.BAD_REQUEST);
        }
        return new Range(start.atStartOfDay(ZONE).toInstant(),
            end.plusDays(1).atStartOfDay(ZONE).toInstant(), start, end);
    }

    /** Báo cáo chi tiết 1 quán (check chủ quán). */
    @Transactional(readOnly = true)
    public ReportDtos.MerchantReport merchantReport(Long merchantId, Long restaurantId,
                                                    LocalDate from, LocalDate to) {
        Restaurant r = restaurants.findById(restaurantId)
            .orElseThrow(() -> new BusinessException("RESTAURANT_NOT_FOUND",
                "Quán không tồn tại", HttpStatus.NOT_FOUND));
        if (!r.getOwnerId().equals(merchantId)) {
            throw new BusinessException("NOT_OWNER", "Bạn không sở hữu quán này", HttpStatus.FORBIDDEN);
        }
        Range rg = range(from, to);
        List<Order> list = orders.findByRestaurantIdAndCreatedAtBetween(
            restaurantId, rg.fromInc(), rg.toExc());
        return buildMerchant(restaurantId, rg, list);
    }

    /** Báo cáo toàn sàn cho admin. */
    @Transactional(readOnly = true)
    public ReportDtos.AdminReport adminReport(LocalDate from, LocalDate to) {
        Range rg = range(from, to);
        List<Order> list = orders.findByCreatedAtBetween(rg.fromInc(), rg.toExc());
        Map<Long, Payment> payMap = new HashMap<>();
        if (!list.isEmpty()) {
            List<Long> ids = list.stream().map(Order::getId).toList();
            for (List<Long> chunk : chunks(ids, 500)) {
                for (Payment p : payments.findByOrderIdIn(chunk)) payMap.put(p.getOrderId(), p);
            }
        }
        Acc acc = accumulate(list);
        List<ReportDtos.DailyPoint> daily = daily(list, rg.from(), rg.to());
        List<ReportDtos.StatusSlice> byStatus = byStatus(list);
        List<ReportDtos.PaySlice> byPayment = byPayment(list, payMap);
        List<ReportDtos.TopRestaurant> topR = topRestaurants(list);
        List<ReportDtos.TopDish> topD = topDishes(list);
        long customers = users.countByRole(Role.CUSTOMER);
        long merchants = users.countByRole(Role.MERCHANT);
        return new ReportDtos.AdminReport(rg.from(), rg.to(),
            acc.total(), acc.completed(), acc.cancelled(),
            acc.revenue(), acc.discount(), acc.avgOrderValue(), acc.cancelRate(),
            customers, merchants, daily, byStatus, byPayment, topR, topD);
    }

    /** Xuất CSV báo cáo quán (giữ đơn giản: 1 dòng/ngày + top món). */
    @Transactional(readOnly = true)
    public String merchantCsv(Long merchantId, Long restaurantId, LocalDate from, LocalDate to) {
        var rep = merchantReport(merchantId, restaurantId, from, to);
        StringBuilder sb = new StringBuilder("date,orders,revenue,discount\n");
        for (var d : rep.daily()) {
            sb.append(d.date()).append(',').append(d.orders()).append(',')
              .append(d.revenue()).append(',').append(d.discount()).append('\n');
        }
        sb.append("\nitem_id,item_name,quantity,revenue\n");
        for (var t : rep.topDishes()) {
            sb.append(t.itemId()).append(",\"").append(t.itemName().replace("\"", "\"\""))
              .append("\",").append(t.quantity()).append(',').append(t.revenue()).append('\n');
        }
        return sb.toString();
    }

    // ---------- nội bộ ----------

    private record Acc(long total, long completed, long cancelled,
                       BigDecimal revenue, BigDecimal discount,
                       BigDecimal avgOrderValue, double cancelRate) {}

    private Acc accumulate(List<Order> list) {
        long total = list.size();
        long completed = list.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelled = list.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();
        BigDecimal revenue = list.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = list.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
            .map(o -> o.getDiscountAmount() == null ? BigDecimal.ZERO : o.getDiscountAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long billed = total - cancelled;
        BigDecimal avg = billed <= 0 ? BigDecimal.ZERO
            : revenue.divide(BigDecimal.valueOf(billed), 0, RoundingMode.HALF_UP);
        double cancelRate = total == 0 ? 0 : (double) cancelled * 100 / total;
        return new Acc(total, completed, cancelled, revenue, discount, avg, cancelRate);
    }

    private List<ReportDtos.DailyPoint> daily(List<Order> list, LocalDate from, LocalDate to) {
        Map<LocalDate, List<Order>> byDay = new HashMap<>();
        for (Order o : list) {
            LocalDate d = LocalDate.ofInstant(o.getCreatedAt(), ZONE);
            byDay.computeIfAbsent(d, k -> new ArrayList<>()).add(o);
        }
        List<ReportDtos.DailyPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            List<Order> day = byDay.getOrDefault(d, List.of());
            long n = day.size();
            BigDecimal rev = day.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal disc = day.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> o.getDiscountAmount() == null ? BigDecimal.ZERO : o.getDiscountAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new ReportDtos.DailyPoint(d, n, rev, disc));
        }
        return out;
    }

    private List<ReportDtos.StatusSlice> byStatus(List<Order> list) {
        Map<OrderStatus, List<Order>> g = new HashMap<>();
        for (Order o : list) g.computeIfAbsent(o.getStatus(), k -> new ArrayList<>()).add(o);
        List<ReportDtos.StatusSlice> out = new ArrayList<>();
        for (var e : g.entrySet()) {
            BigDecimal rev = e.getValue().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new ReportDtos.StatusSlice(e.getKey().name(), e.getValue().size(), rev));
        }
        out.sort(Comparator.comparing(ReportDtos.StatusSlice::status));
        return out;
    }

    private List<ReportDtos.PaySlice> byPayment(List<Order> list, Map<Long, Payment> payMap) {
        Map<String, Long> g = new HashMap<>();
        for (Order o : list) {
            Payment p = payMap.get(o.getId());
            String key = (p == null ? "UNKNOWN" : p.getMethod().name())
                + "/" + (p == null ? "UNKNOWN" : p.getStatus().name());
            g.merge(key, 1L, Long::sum);
        }
        List<ReportDtos.PaySlice> out = new ArrayList<>();
        for (var e : g.entrySet()) {
            String[] parts = e.getKey().split("/", 2);
            out.add(new ReportDtos.PaySlice(parts[0], parts[1], e.getValue()));
        }
        out.sort(Comparator.comparing(ReportDtos.PaySlice::method)
            .thenComparing(ReportDtos.PaySlice::status));
        return out;
    }

    private List<ReportDtos.TopDish> topDishes(List<Order> list) {
        List<Order> billed = list.stream()
            .filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        if (billed.isEmpty()) return List.of();
        List<Long> ids = billed.stream().map(Order::getId).toList();
        Map<Long, Agg> g = new HashMap<>();
        for (List<Long> chunk : chunks(ids, 500)) {
            for (OrderItem i : orderItems.findByOrderIdIn(chunk)) {
                Agg a = g.computeIfAbsent(i.getItemId(), k -> new Agg(i.getItemName()));
                a.qty += i.getQuantity();
                a.revenue = a.revenue.add(i.lineTotal());
            }
        }
        return g.entrySet().stream()
            .map(e -> new ReportDtos.TopDish(e.getKey(), e.getValue().name,
                e.getValue().qty, e.getValue().revenue))
            .sorted(Comparator.comparingLong(ReportDtos.TopDish::quantity).reversed())
            .limit(10).toList();
    }

    private List<ReportDtos.TopRestaurant> topRestaurants(List<Order> list) {
        Map<Long, List<Order>> g = new HashMap<>();
        for (Order o : list) {
            if (o.getStatus() == OrderStatus.CANCELLED) continue;
            g.computeIfAbsent(o.getRestaurantId(), k -> new ArrayList<>()).add(o);
        }
        if (g.isEmpty()) return List.of();
        Map<Long, Restaurant> rm = new HashMap<>();
        for (List<Long> chunk : chunks(new ArrayList<>(g.keySet()), 500)) {
            for (Restaurant r : restaurants.findAllById(chunk)) rm.put(r.getId(), r);
        }
        List<ReportDtos.TopRestaurant> out = new ArrayList<>();
        for (var e : g.entrySet()) {
            Restaurant r = rm.get(e.getKey());
            BigDecimal rev = e.getValue().stream()
                .map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            out.add(new ReportDtos.TopRestaurant(e.getKey(),
                r == null ? ("Quán #" + e.getKey()) : r.getName(),
                e.getValue().size(), rev,
                r == null || r.getAverageRating() == null ? 0 : r.getAverageRating().doubleValue(),
                r == null ? 0 : r.getRatingCount()));
        }
        out.sort(Comparator.comparing(ReportDtos.TopRestaurant::revenue).reversed());
        return out.stream().limit(10).toList();
    }

    private ReportDtos.MerchantReport buildMerchant(Long restaurantId, Range rg, List<Order> list) {
        Acc acc = accumulate(list);
        return new ReportDtos.MerchantReport(restaurantId, rg.from(), rg.to(),
            acc.total(), acc.completed(), acc.cancelled(),
            acc.revenue(), acc.discount(), acc.avgOrderValue(), acc.cancelRate(),
            daily(list, rg.from(), rg.to()), byStatus(list), topDishes(list));
    }

    private static class Agg {
        String name; long qty; BigDecimal revenue = BigDecimal.ZERO;
        Agg(String name) { this.name = name; }
    }

    private static <T> List<List<T>> chunks(List<T> all, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i += size) out.add(all.subList(i, Math.min(i + size, all.size())));
        return out;
    }
}
