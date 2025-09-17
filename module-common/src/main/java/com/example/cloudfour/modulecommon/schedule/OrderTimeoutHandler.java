package com.example.cloudfour.modulecommon.schedule;

import com.example.cloudfour.modulecommon.converter.OrderCommandConverter;
import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.example.cloudfour.modulecommon.messaging.order.OrderCommands;
import com.example.cloudfour.modulecommon.messaging.order.OrderEvents;
import com.example.cloudfour.modulecommon.outbox.entity.OutboxEvent;
import com.example.cloudfour.modulecommon.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutHandler {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final OutboxService outboxService;

    @Value("${kafka.topics.orderCommands:order.commands.v1}")
    private String orderCommandsTopic;

    public void handleOrderTimeout(String orderId) {
        try {
            log.warn("주문 타임아웃 처리 시작: orderId={}", orderId);

            OrderInfo orderInfo = getOrderInfo(orderId);

            String reason = "주문 타임아웃: 결제가 10분 내에 완료되지 않았습니다";

            OrderCommands.CancelOrder cancelCommand = OrderCommandConverter.toCancelOrderCommand(
                    orderId,
                    orderInfo.userId() != null ? orderInfo.userId().toString() : null,
                    orderInfo.storeId() != null ? orderInfo.storeId().toString() : null,
                    reason
            );

            outboxService.saveEvent(
                    orderId,
                    "Order",
                    "CancelOrder",
                    cancelCommand,
                    orderCommandsTopic,
                    orderId
            );

            log.info("주문 타임아웃 → CancelOrder 커맨드 저장(outbox): orderId={}, userId={}, storeId={}",
                    orderId, orderInfo.userId(), orderInfo.storeId());

        } catch (Exception e) {
            log.error("주문 타임아웃 처리 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new RuntimeException("주문 타임아웃 처리 실패", e);
        }
    }

    private OrderInfo getOrderInfo(String orderId) {
        try {
            List<OutboxEvent> orderCreatedEvents = outboxEventRepository
                    .findByAggregateIdAndEventType(orderId, "OrderCreated");
            
            if (orderCreatedEvents.isEmpty()) {
                log.warn("OrderCreated 이벤트를 찾을 수 없습니다: orderId={}", orderId);
                return new OrderInfo(null, null);
            }

            OutboxEvent latestEvent = orderCreatedEvents.get(orderCreatedEvents.size() - 1);

            String eventData = latestEvent.getEventData();
            OrderEvents.OrderCreated orderCreated = objectMapper.readValue(eventData, OrderEvents.OrderCreated.class);
            
            return new OrderInfo(orderCreated.getUserId(), orderCreated.getStoreId());
            
        } catch (Exception e) {
            log.error("주문 정보 조회 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            return new OrderInfo(null, null);
        }
    }

    private record OrderInfo(UUID userId, UUID storeId) {}
}
