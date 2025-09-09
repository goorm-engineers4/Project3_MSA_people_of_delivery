package com.example.cloudfour.storeservice.domain.menu.dto;

import com.example.cloudfour.storeservice.domain.common.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderEvent {
    @Getter
    @Builder
    public static class OrderCompletedEvent{
        private UUID orderId;
        private UUID userId;
        private UUID paymentId;
        private Long quantity;
        private List<OrderItem> orderItems;
        private LocalDateTime completedAt;
        private OrderStatus status;

        @Getter
        @Builder
        public static class OrderItem {
            private UUID menuId;
            private String menuName;
            private Long quantity;
            private Integer price;
        }
    }
}
