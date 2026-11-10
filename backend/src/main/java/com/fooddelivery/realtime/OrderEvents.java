package com.fooddelivery.realtime;

import com.fooddelivery.order.OrderDtos;
import com.fooddelivery.order.OrderStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Phát sự kiện đơn hàng qua WebSocket STOMP. */
@Component
public class OrderEvents {
    private final SimpMessagingTemplate ws;

    public OrderEvents(SimpMessagingTemplate ws) {
        this.ws = ws;
    }

    public record OrderEvent(Long orderId, Long restaurantId, OrderStatus status,
                             String payStatus, long at) {}

    private void send(OrderDtos.OrderResponse o) {
        var ev = new OrderEvent(o.id(), o.restaurantId(), o.status(),
            o.payStatus() == null ? null : o.payStatus().name(), System.currentTimeMillis());
        // Kênh khách: theo dõi đơn của mình
        ws.convertAndSend("/topic/orders/" + o.id(), ev);
        // Kênh quán: merchant nghe đơn mới / đổi trạng thái
        ws.convertAndSend("/topic/merchant/" + o.restaurantId(), ev);
    }

    public void placed(OrderDtos.OrderResponse o) { send(o); }
    public void statusChanged(OrderDtos.OrderResponse o) { send(o); }
    public void paymentChanged(OrderDtos.OrderResponse o) { send(o); }

    public void paymentChanged(Long orderId, Long restaurantId, String payStatus) {
        var ev = new OrderEvent(orderId, restaurantId, null, payStatus, System.currentTimeMillis());
        ws.convertAndSend("/topic/orders/" + orderId, ev);
        ws.convertAndSend("/topic/merchant/" + restaurantId, ev);
    }
}
