package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.example.cloudfour.modulecommon.messaging.MessageConsumer;
import com.example.cloudfour.modulecommon.messaging.SagaAwareDLQHandler;
import com.example.cloudfour.modulecommon.messaging.inventory.InventoryCommands;
import com.example.cloudfour.modulecommon.messaging.inventory.InventoryEvents;
import com.example.cloudfour.modulecommon.idempotency.MessageIdempotencyService;
import com.example.cloudfour.modulecommon.messaging.payment.PaymentEvents;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import com.example.cloudfour.storeservice.domain.menu.service.command.StockCommandService;
import com.example.cloudfour.storeservice.domain.menu.service.RedisInventoryService;
import com.example.cloudfour.storeservice.domain.menu.service.event.InventoryEventService;
import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.exception.StockException;
import com.example.cloudfour.storeservice.domain.menu.exception.StockErrorCode;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandHandler {
    
    private final StockCommandService stockCommandService;
    private final RedisInventoryService redisInventoryService;
    private final InventoryEventService inventoryEventService;
    private final MenuRepository menuRepository;
    private final MessageConsumer messageConsumer;
    private final SagaAwareDLQHandler sagaAwareDLQHandler;
    private final ObjectMapper objectMapper;
    private final MessageIdempotencyService idempotencyService;
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${kafka.topics.inventoryEvents:inventory.events.v1}")
    private String inventoryEventsTopic;

    @KafkaListener(topics = "${kafka.topics.inventoryCommands:inventory.commands.v1}", 
                   groupId = "inventory-command-handler")
    public void handleInventoryCommand(
            @Payload Envelope<?> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            if (envelope == null || envelope.getMeta() == null) {
                log.warn("유효하지 않은 메시지(envelope/meta null) 수신: topic={}, key={}", topic, key);
                throw new IllegalArgumentException("유효하지 않은 메시지(envelope/meta null)");
            }
            messageConsumer.logMessageReceived(envelope, topic, partition, offset, key);
            if (!idempotencyService.markIfNotProcessed("inventory-command-handler", envelope.getMeta().getMsgId(), topic)) {
                log.warn("중복 이벤트 스킵: consumer=inventory-command-handler, msgId={}", envelope.getMeta().getMsgId());
                acknowledgment.acknowledge();
                return;
            }
            
            Object payload = envelope.getPayload();

            if (payload instanceof Map) {
                payload = convertLinkedHashMapToCommand((Map<String, Object>) payload, envelope.getMeta().getType());
            }
            
                        if (payload instanceof InventoryCommands.ReserveInventory) {
                            handleReserveInventory((InventoryCommands.ReserveInventory) payload);
                            acknowledgment.acknowledge();
                        } else if (payload instanceof InventoryCommands.ReleaseInventory) {
                            handleReleaseInventory((InventoryCommands.ReleaseInventory) payload);
                            acknowledgment.acknowledge();
                        } else {
                            throw new IllegalArgumentException("알 수 없는 인벤토리 커맨드 타입: " + payload.getClass().getSimpleName());
                        }
            
        } catch (Exception e) {
            String msgId = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getMsgId() : null;
            String sagaId = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getSagaId() : null;
            String type = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getType() : null;
            messageConsumer.logMessageProcessingError(
                    topic, key, msgId, sagaId, type, e);
            
            log.error("인벤토리 커맨드 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @KafkaListener(topics = "${kafka.topics.paymentEvents:payment.events.v1}", 
                   groupId = "inventory-payment-event-handler")
    public void handlePaymentEvent(
            @Payload Envelope<?> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            if (envelope == null || envelope.getMeta() == null) {
                log.warn("유효하지 않은 메시지(envelope/meta null) 수신: topic={}, key={}", topic, key);
                throw new IllegalArgumentException("유효하지 않은 메시지(envelope/meta null)");
            }
            messageConsumer.logMessageReceived(envelope, topic, partition, offset, key);
            if (!idempotencyService.markIfNotProcessed("inventory-payment-event-handler", envelope.getMeta().getMsgId(), topic)) {
                log.warn("중복 이벤트 스킵: consumer=inventory-payment-event-handler, msgId={}", envelope.getMeta().getMsgId());
                acknowledgment.acknowledge();
                return;
            }
            
            Object payload = envelope.getPayload();
            String eventType = envelope.getMeta().getType();

            if ("PaymentAuthorized".equals(eventType)) {
                handleCommitInventoryFromAuthorized(payload);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            String msgId = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getMsgId() : null;
            String sagaId = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getSagaId() : null;
            String type = (envelope != null && envelope.getMeta() != null) ? envelope.getMeta().getType() : null;
            messageConsumer.logMessageProcessingError(
                    topic, key, msgId, sagaId, type, e);
            
            log.error("결제 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }
    
    private Object convertLinkedHashMapToCommand(Map<String, Object> map, String type) {
        try {
            if ("ReserveInventory".equals(type)) {
                return objectMapper.convertValue(map, InventoryCommands.ReserveInventory.class);
            } else if ("ReleaseInventory".equals(type)) {
                return objectMapper.convertValue(map, InventoryCommands.ReleaseInventory.class);
            }
            throw new IllegalArgumentException("지원하지 않는 커맨드 타입: " + type);
        } catch (Exception e) {
            log.error("커맨드 변환 실패: type={}, error={}", type, e.getMessage(), e);
            throw e;
        }
    }
    
    private void handleReserveInventory(InventoryCommands.ReserveInventory command) {
        log.info("재고 예약 처리 시작: orderId={}, storeId={}", command.getOrderId(), command.getStoreId());
        
        try {
            for (InventoryCommands.ReserveInventory.ReserveItem item : command.getItems()) {
                var result = redisInventoryService.reserveStock(
                    item.getMenuId(), 
                    item.getQuantity().longValue(), 
                    command.getOrderId()
                );
                
                if (!result.isSuccess()) {
                    log.warn("재고 예약 실패: menuId={}, quantity={}, reason={}", 
                            item.getMenuId(), item.getQuantity(), result.getMessage());

                    inventoryEventService.publishInventoryReservationFailed(
                        command.getOrderId(), 
                        command.getStoreId(), 
                        List.of(InventoryEvents.InventoryReservationFailed.FailedItem.builder()
                            .menuId(item.getMenuId())
                            .menuName(getMenuName(item.getMenuId()))
                            .requestedQuantity(item.getQuantity())
                            .availableQuantity(result.getRemainingStock().intValue())
                            .build())
                    );
                    return;
                }
            }

            List<InventoryEvents.InventoryReserved.ReservedItem> reservedItems = command.getItems().stream()
                .map(item -> InventoryEvents.InventoryReserved.ReservedItem.builder()
                    .menuId(item.getMenuId())
                    .menuName(getMenuName(item.getMenuId()))
                    .quantity(item.getQuantity().intValue())
                    .reservedQuantity(item.getQuantity().intValue())
                    .build())
                .toList();
                
            inventoryEventService.publishInventoryReserved(
                command.getOrderId(), 
                command.getStoreId(), 
                reservedItems
            );
            
            log.info("재고 예약 완료: orderId={}, storeId={}", command.getOrderId(), command.getStoreId());
            
        } catch (Exception e) {
            log.error("재고 예약 처리 중 오류: orderId={}, error={}", command.getOrderId(), e.getMessage(), e);
            throw new StockException(StockErrorCode.INTERNAL_ERROR);
        }
    }
    
    private void handleReleaseInventory(InventoryCommands.ReleaseInventory command) {
        log.info("재고 해제 이벤트 발행 시작: orderId={}", command.getOrderId());

        try {
            List<InventoryEvents.InventoryReleased.ReleasedItem> releasedItems =
                    (command.getItems() == null ? java.util.Collections.<InventoryCommands.ReleaseInventory.ReleaseItem>emptyList() : command.getItems())
                            .stream()
                            .map(item -> InventoryEventConverter.createReleasedItem(
                                    item.getMenuId(),
                                    getMenuName(item.getMenuId()),
                                    item.getQuantity()))
                            .toList();

            inventoryEventService.publishInventoryReleased(
                    command.getOrderId(),
                    command.getStoreId(),
                    releasedItems
            );

            log.info("재고 해제 이벤트 발행 완료: orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("재고 해제 이벤트 발행 중 오류: orderId={}, error={}", command.getOrderId(), e.getMessage(), e);
            throw new StockException(StockErrorCode.INTERNAL_ERROR);
        }
    }
    
    private void handleCommitInventory(Object event) {
        log.info("재고 commit 처리 시작: event={}", event);
        
        try {
            if (event instanceof PaymentEvent.PaymentCompletedEvent paymentEvent) {
                List<PaymentEvent.PaymentCompletedEvent.OrderItem> orderItems = paymentEvent.getOrderItems();
 
                boolean success = stockCommandService.decreaseListStock(orderItems);
                
                if (success) {
                    List<InventoryEvents.InventoryCommitted.CommittedItem> committedItems = orderItems.stream()
                        .map(item -> InventoryEvents.InventoryCommitted.CommittedItem.builder()
                            .menuId(item.getMenuId())
                            .menuName(getMenuName(item.getMenuId()))
                            .quantity(item.getQuantity().intValue())
                            .build())
                        .toList();

                    UUID storeId = orderItems.isEmpty() ? null : getStoreId(orderItems.get(0).getMenuId());
                    
                    inventoryEventService.publishInventoryCommitted(
                        paymentEvent.getOrderId(),
                        storeId,
                        committedItems
                    );
                    
                    log.info("재고 commit 완료: orderId={}, paymentId={}", 
                            paymentEvent.getOrderId(), paymentEvent.getPaymentId());
                } else {
                    UUID storeId = orderItems.isEmpty() ? null : getStoreId(orderItems.get(0).getMenuId());
                    
                    inventoryEventService.publishInventoryCommitFailed(
                        paymentEvent.getOrderId(),
                        storeId,
                        "재고 차감 실패"
                    );
                    
                    log.warn("재고 commit 실패: orderId={}, paymentId={}", 
                            paymentEvent.getOrderId(), paymentEvent.getPaymentId());
                }
            } else {
                log.warn("알 수 없는 이벤트 타입: {}", event.getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            log.error("재고 commit 처리 중 오류: event={}, error={}", event, e.getMessage(), e);
            throw new StockException(StockErrorCode.INTERNAL_ERROR);
        }
    }

    private void handleCommitInventoryFromAuthorized(Object payload) {
        log.info("PaymentAuthorized 수신에 따른 재고 commit 처리 시작");
        try {
            PaymentEvents.PaymentAuthorized event;
            if (payload instanceof LinkedHashMap) {
                event = objectMapper.convertValue(payload, PaymentEvents.PaymentAuthorized.class);
            } else {
                event = (PaymentEvents.PaymentAuthorized) payload;
            }

            UUID orderId = event.getOrderId();
            UUID storeId = event.getStoreId();

            List<PaymentEvent.PaymentCompletedEvent.OrderItem> orderItems = fetchReservedItemsFromRedis(orderId);
            if (orderItems.isEmpty()) {
                log.warn("주문별 예약 정보가 없습니다: orderId={}", orderId);
                inventoryEventService.publishInventoryCommitFailed(orderId, storeId, "예약 정보 없음");
                return;
            }

            boolean success = stockCommandService.decreaseListStock(orderItems);
            if (success) {
                List<InventoryEvents.InventoryCommitted.CommittedItem> committedItems = orderItems.stream()
                        .map(item -> InventoryEvents.InventoryCommitted.CommittedItem.builder()
                                .menuId(item.getMenuId())
                                .menuName(getMenuName(item.getMenuId()))
                                .quantity(item.getQuantity().intValue())
                                .build())
                        .toList();
                inventoryEventService.publishInventoryCommitted(orderId, storeId, committedItems);
                log.info("재고 commit 완료: orderId={}", orderId);
            } else {
                inventoryEventService.publishInventoryCommitFailed(orderId, storeId, "재고 차감 실패");
                log.warn("재고 commit 실패: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("PaymentAuthorized 기반 재고 commit 처리 중 오류: error={}", e.getMessage(), e);
            throw new StockException(StockErrorCode.INTERNAL_ERROR);
        }
    }

    private List<PaymentEvent.PaymentCompletedEvent.OrderItem>
    fetchReservedItemsFromRedis(UUID orderId) {
        final String ORDER_RESERVATION_KEY_PREFIX = "order:rsrv:";
        String orderReservationKey = ORDER_RESERVATION_KEY_PREFIX + orderId;
        try {
            HashOperations<String, String, String> ops = redisTemplate.opsForHash();
            Map<String, String> map = ops.entries(orderReservationKey);
            List<PaymentEvent.PaymentCompletedEvent.OrderItem> items = new ArrayList<>();
            for (var entry : map.entrySet()) {
                String menuIdStr = entry.getKey();
                String qtyStr = entry.getValue();
                try {
                    UUID menuId = UUID.fromString(menuIdStr);
                    Long qty = Long.valueOf(qtyStr);
                    UUID stockId = menuRepository.findById(menuId)
                            .map(m -> m.getStock().getId())
                            .orElseThrow(() -> new StockException(StockErrorCode.NOT_FOUND));
                    var item = PaymentEvent.PaymentCompletedEvent.OrderItem.builder()
                            .menuId(menuId)
                            .stockId(stockId)
                            .menuName(getMenuName(menuId))
                            .quantity(qty)
                            .price(null)
                            .build();
                    items.add(item);
                } catch (Exception ignore) {}
            }
            return items;
        } catch (Exception e) {
            log.error("Redis 주문 예약 항목 조회 실패 - orderId: {}", orderId, e);
            return Collections.emptyList();
        }
    }
    
    private String getMenuName(UUID menuId) {
        try {
            return menuRepository.findById(menuId)
                .map(menu -> menu.getName())
                .orElse("알 수 없는 메뉴");
        } catch (Exception e) {
            log.warn("메뉴명 조회 실패: menuId={}, error={}", menuId, e.getMessage());
            return "알 수 없는 메뉴";
        }
    }
    
    private UUID getStoreId(UUID menuId) {
        try {
            return menuRepository.findById(menuId)
                .map(menu -> menu.getStore().getId())
                .orElse(null);
        } catch (Exception e) {
            log.warn("가게 ID 조회 실패: menuId={}, error={}", menuId, e.getMessage());
            return null;
        }
    }
}
