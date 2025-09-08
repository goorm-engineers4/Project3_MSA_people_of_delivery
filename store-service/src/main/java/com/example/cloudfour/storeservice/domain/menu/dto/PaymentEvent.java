package com.example.cloudfour.storeservice.domain.menu.dto;

import com.example.cloudfour.storeservice.domain.common.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PaymentEvent {

    @Getter
    @Builder
    public static class PaymentCompletedEvent{
        private UUID orderId;
        private UUID userId;
        private UUID paymentId;
        private Long quantity;
        private List<OrderItem> orderItems;
        private LocalDateTime completedAt;
        private PaymentStatus status;

        @Getter
        @Builder
        public static class OrderItem {
            private UUID menuId;
            private UUID stockId;
            private String menuName;
            private Long quantity;
            private Integer price;
        }
    }

    @Getter
    @Builder
    public static class PaymentFailedEvent{
        private UUID orderId;
        private UUID userId;
        private UUID paymentId;
        private LocalDateTime failedAt;
    }

    @Getter
    @Builder
    public static class PaymentCanceledEvent{
        private UUID orderId;
        private LocalDateTime cancelledAt;
    }
}
