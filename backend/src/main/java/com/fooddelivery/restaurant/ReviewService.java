package com.fooddelivery.restaurant;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.order.Order;
import com.fooddelivery.order.OrderRepository;
import com.fooddelivery.order.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final ReviewRepository reviews;
    private final RestaurantRepository restaurants;
    private final OrderRepository orders;

    public ReviewService(ReviewRepository reviews, RestaurantRepository restaurants, OrderRepository orders) {
        this.reviews = reviews;
        this.restaurants = restaurants;
        this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDtos.ReviewResponse> listByRestaurant(Long restaurantId, int page, int size) {
        return reviews.findByRestaurantIdOrderByCreatedAtDesc(
                restaurantId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
            .map(ReviewDtos.ReviewResponse::from);
    }

    /** Chỉ được đánh giá khi đơn COMPLETED, mỗi đơn 1 lần, đúng chủ đơn. */
    @Transactional
    public ReviewDtos.ReviewResponse create(Long userId, ReviewDtos.CreateReviewRequest req) {
        Order order = orders.findById(req.orderId())
            .orElseThrow(() -> new BusinessException("ORDER_NOT_FOUND",
                "Đơn hàng không tồn tại", HttpStatus.NOT_FOUND));
        if (!order.ownedBy(userId)) {
            throw new BusinessException("NOT_ORDER_OWNER",
                "Bạn không sở hữu đơn hàng này", HttpStatus.FORBIDDEN);
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("ORDER_NOT_COMPLETED",
                "Chỉ được đánh giá khi đơn đã hoàn thành", HttpStatus.BAD_REQUEST);
        }
        if (reviews.existsByOrderId(req.orderId())) {
            throw new BusinessException("REVIEW_EXISTS",
                "Đơn này đã được đánh giá", HttpStatus.CONFLICT);
        }
        Restaurant restaurant = restaurants.findById(order.getRestaurantId())
            .orElseThrow(() -> new BusinessException("RESTAURANT_NOT_FOUND",
                "Nhà hàng không tồn tại", HttpStatus.NOT_FOUND));
        Review review = new Review(order.getId(), userId, restaurant.getId(), req.rating(), req.comment());
        reviews.save(review);
        restaurant.addRating(req.rating());
        return ReviewDtos.ReviewResponse.from(review);
    }
}
