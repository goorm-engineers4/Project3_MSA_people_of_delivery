package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.example.cloudfour.modulecommon.messaging.MessageConsumer;
import com.example.cloudfour.modulecommon.messaging.inventory.InventoryCommands;
import com.example.cloudfour.modulecommon.messaging.inventory.InventoryEvents;
import com.example.cloudfour.modulecommon.messaging.payment.PaymentEvents;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.example.cloudfour.storeservice.domain.menu.converter.InventoryEventConverter;
import com.example.cloudfour.storeservice.domain.menu.service.command.StockCommandService;
import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.entity.Menu;
import com.example.cloudfour.storeservice.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCommandHandler {
    
    private final OutboxService outboxService;
    private final MessageConsumer messageConsumer;
    private final RedisInventoryService redisInventoryService;
    private final StockCommandService stockCommandService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MenuRepository menuRepository;
    
    @Value("${kafka.topics.inventoryEvents:inventory.events.v1}")
    private String inventoryEventsTopic;

    @KafkaListener(topics = "${kafka.topics.inventoryCommands:inventory.commands.v1}", 
                   groupId = "inventory-command-handler")
    @Transactional
    public void handleInventoryCommand(
            @Payload com.example.cloudfour.modulecommon.messaging.Envelope<?> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            messageConsumer.logMessageReceived(envelope, topic, partition, offset, key);
            
            Object payload = envelope.getPayload();

            if (payload instanceof Map) {
                payload = convertLinkedHashMapToCommand((Map<String, Object>) payload, envelope.getMeta().getType());
            }
            
            if (payload instanceof InventoryCommands.ReserveInventory) {
                handleReserveInventory((InventoryCommands.ReserveInventory) payload, acknowledgment);
            } else if (payload instanceof InventoryCommands.ReleaseInventory) {
                handleReleaseInventory((InventoryCommands.ReleaseInventory) payload, acknowledgment);
            } else {
                log.warn("알 수 없는 인벤토리 커맨드 타입: {}", payload.getClass().getSimpleName());
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            messageConsumer.logMessageProcessingError(
                    topic, key, envelope.getMeta().getMsgId(), 
                    envelope.getMeta().getSagaId(), 
                    envelope.getMeta().getType(), e);
            
            log.error("인벤토리 커맨드 처리 실패: error={}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
    
    private void handleReserveInventory(InventoryCommands.ReserveInventory command, Acknowledgment acknowledgment) {
        UUID orderId = command.getOrderId();
        UUID storeId = command.getStoreId();
        
        log.info("재고 예약 커맨드 처리 시작: orderId={}, storeId={}", orderId, storeId);

        List<InventoryEvents.InventoryReserved.ReservedItem> reservedItems = new ArrayList<>();
        List<InventoryEvents.InventoryReservationFailed.FailedItem> failedItems = new ArrayList<>();

        for (InventoryCommands.ReserveInventory.ReserveItem item : command.getItems()) {
            try {
                var reserveResult = redisInventoryService.reserveStock(
                        item.getMenuId(), 
                        item.getQuantity().longValue(), 
                        orderId
                );
                
                if (reserveResult.isSuccess()) {
                    reservedItems.add(InventoryEventConverter.createReservedItem(
                            item.getMenuId(), item.getMenuName(), item.getQuantity()));
                    log.info("재고 예약 성공: menuId={}, quantity={}", item.getMenuId(), item.getQuantity());
                } else {
                    Integer availableQuantity = reserveResult.getRemainingStock() != null ? 
                            reserveResult.getRemainingStock().intValue() : 0;
                    
                    failedItems.add(InventoryEventConverter.createFailedItem(
                            item.getMenuId(), item.getMenuName(), item.getQuantity(), availableQuantity));
                    log.warn("재고 예약 실패: menuId={}, requested={}, available={}, reason={}", 
                            item.getMenuId(), item.getQuantity(), availableQuantity, reserveResult.getMessage());
                }
            } catch (Exception e) {
                failedItems.add(InventoryEventConverter.createFailedItem(
                        item.getMenuId(), item.getMenuName(), item.getQuantity(), 0));
                log.error("재고 예약 중 오류: menuId={}, quantity={}", item.getMenuId(), item.getQuantity(), e);
            }
        }

        if (!failedItems.isEmpty()) {
            String reason = failedItems.size() == command.getItems().size() ? 
                    "모든 재고 예약 실패" : "일부 재고 예약 실패";
            
            InventoryEvents.InventoryReservationFailed event = InventoryEventConverter.createInventoryReservationFailedEvent(
                    orderId, storeId, reason, failedItems);
            
            outboxService.saveEvent(
                    orderId.toString(),
                    "Inventory",
                    "InventoryReservationFailed",
                    event,
                    inventoryEventsTopic,
                    orderId.toString()
            );
            
            log.warn("재고 예약 실패 이벤트 발행: orderId={}, failedItems={}", orderId, failedItems.size());
        } else {
            InventoryEvents.InventoryReserved event = InventoryEventConverter.createInventoryReservedEvent(
                    orderId, storeId, reservedItems);
            
            outboxService.saveEvent(
                    orderId.toString(),
                    "Inventory",
                    "InventoryReserved",
                    event,
                    inventoryEventsTopic,
                    orderId.toString()
            );
            
            log.info("재고 예약 성공 이벤트 발행: orderId={}, reservedItems={}", orderId, reservedItems.size());
        }
        
        acknowledgment.acknowledge();
    }
    
    private void handleReleaseInventory(InventoryCommands.ReleaseInventory command, Acknowledgment acknowledgment) {
        UUID orderId = command.getOrderId();
        UUID storeId = command.getStoreId();
        
        log.info("재고 해제 커맨드 처리 시작: orderId={}, storeId={}", orderId, storeId);
        
        List<InventoryEvents.InventoryReleased.ReleasedItem> releasedItems = new ArrayList<>();
        
        try {
                redisInventoryService.releaseReservation(orderId);

            for (InventoryCommands.ReleaseInventory.ReleaseItem item : command.getItems()) {
                releasedItems.add(InventoryEventConverter.createReleasedItem(
                        item.getMenuId(), item.getMenuName(), item.getQuantity()));
            }
            
            log.info("재고 해제 성공: orderId={}, items={}", orderId, command.getItems().size());
            
        } catch (Exception e) {
            log.error("재고 해제 중 오류: orderId={}", orderId, e);

            for (InventoryCommands.ReleaseInventory.ReleaseItem item : command.getItems()) {
                releasedItems.add(InventoryEventConverter.createReleasedItem(
                        item.getMenuId(), item.getMenuName(), item.getQuantity()));
            }
        }

        InventoryEvents.InventoryReleased event = InventoryEventConverter.createInventoryReleasedEvent(
                orderId, storeId, releasedItems);
        
        outboxService.saveEvent(
                orderId.toString(),
                "Inventory",
                "InventoryReleased",
                event,
                inventoryEventsTopic,
                orderId.toString()
        );
        
        log.info("재고 해제 이벤트 발행 완료: orderId={}, releasedItems={}", orderId, releasedItems.size());
        acknowledgment.acknowledge();
    }

    @KafkaListener(topics = "${kafka.topics.paymentEvents:payment.events.v1}", 
                   groupId = "inventory-payment-handler")
    @Transactional
    public void handlePaymentAuthorized(
            @Payload Envelope<?> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            messageConsumer.logMessageReceived(envelope, topic, partition, offset, key);
            
            Object payload = envelope.getPayload();
            String eventType = envelope.getMeta().getType();

            if (payload instanceof Map) {
                payload = convertLinkedHashMapToPaymentEvent((Map<String, Object>) payload, eventType);
            }
            
            if (payload instanceof PaymentEvents.PaymentAuthorized) {
                handlePaymentAuthorizedEvent((PaymentEvents.PaymentAuthorized) payload, acknowledgment);
                acknowledgment.acknowledge();
            } else {
                log.debug("결제 이벤트가 아닙니다: {}", payload.getClass().getSimpleName());
                acknowledgment.acknowledge();
            }
            
        } catch (Exception e) {
            messageConsumer.logMessageProcessingError(
                    topic, key, envelope.getMeta().getMsgId(), 
                    envelope.getMeta().getSagaId(), 
                    envelope.getMeta().getType(), e);
            
            log.error("결제 이벤트 처리 실패: error={}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
    
    private void handlePaymentAuthorizedEvent(PaymentEvents.PaymentAuthorized event, Acknowledgment acknowledgment) {
        UUID orderId = event.getOrderId();
        UUID storeId = event.getStoreId();
        UUID userId = event.getUserId();
        BigDecimal totalAmount = event.getAmount();
        
        log.info("결제 승인으로 인한 재고 commit 시작: orderId={}, storeId={}, userId={}, amount={}", 
                orderId, storeId, userId, totalAmount);
        
        try {
            String orderReservationKey = "order:reservation:" + orderId;
            Map<Object, Object> rawData = redisTemplate.opsForHash().entries(orderReservationKey);
            
            if (rawData.isEmpty()) {
                log.warn("Redis에 예약 정보가 없습니다: orderId={}", orderId);
                acknowledgment.acknowledge();
                return;
            }

            List<PaymentEvent.PaymentCompletedEvent.OrderItem> orderItems = new ArrayList<>();
            
            for (Map.Entry<Object, Object> entry : rawData.entrySet()) {
                String menuIdStr = entry.getKey().toString();
                String quantityStr = entry.getValue().toString();
                
                try {
                    UUID menuId = UUID.fromString(menuIdStr);
                    Integer quantity = Integer.parseInt(quantityStr);
                    

                    Menu menu = menuRepository.findById(menuId)
                            .orElseThrow(() -> new RuntimeException("메뉴를 찾을 수 없습니다: " + menuId));
                    UUID stockId = menu.getStock().getId();

                    PaymentEvent.PaymentCompletedEvent.OrderItem orderItem = 
                            PaymentEvent.PaymentCompletedEvent.OrderItem.builder()
                                    .menuId(menuId)
                                    .stockId(stockId)
                                    .menuName(menu.getName())
                                    .quantity(quantity.longValue())
                                    .price(menu.getPrice())
                                    .build();
                    orderItems.add(orderItem);
                    
                } catch (Exception e) {
                    log.error("예약 정보 파싱 실패: menuId={}, quantity={}", menuIdStr, quantityStr, e);
                }
            }

            stockCommandService.decreaseListStock(orderItems);

            List<InventoryEvents.InventoryCommitted.CommittedItem> eventItems = new ArrayList<>();
            for (PaymentEvent.PaymentCompletedEvent.OrderItem orderItem : orderItems) {
                eventItems.add(InventoryEventConverter.createCommittedItem(
                        orderItem.getMenuId(), orderItem.getMenuName(), orderItem.getQuantity().intValue()));
            }
            
            InventoryEvents.InventoryCommitted committedEvent = InventoryEventConverter.createInventoryCommittedEvent(
                    orderId, storeId, eventItems);
            
            outboxService.saveEvent(
                    orderId.toString(),
                    "Inventory",
                    "InventoryCommitted",
                    committedEvent,
                    inventoryEventsTopic,
                    orderId.toString()
            );
            
            log.info("재고 commit 완료 및 이벤트 발행: orderId={}, orderItems={}", 
                    orderId, orderItems.size());

            log.info("재고 commit 완료: orderId={}, orderItems={}", orderId, orderItems.size());
            
        } catch (Exception e) {
            log.error("재고 commit 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            publishInventoryCommitFailedEvent(orderId, storeId, e.getMessage());
        }
        
        acknowledgment.acknowledge();
    }

    private void publishInventoryCommitFailedEvent(UUID orderId, UUID storeId, String reason) {
        try {
            InventoryEvents.InventoryCommitFailed commitFailedEvent = InventoryEventConverter.createInventoryCommitFailedEvent(
                    orderId, storeId, reason);
            
            outboxService.saveEvent(
                    orderId.toString(),
                    "Inventory",
                    "InventoryCommitFailed",
                    commitFailedEvent,
                    inventoryEventsTopic,
                    orderId.toString()
            );
            
            log.info("InventoryCommitFailed 이벤트 발행 완료: orderId={}, storeId={}, reason={}", 
                    orderId, storeId, reason);
            
        } catch (Exception e) {
            log.error("InventoryCommitFailed 이벤트 발행 실패: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }
    
    private Object convertLinkedHashMapToCommand(Map<String, Object> map, String type) {
        try {
            if ("ReserveInventory".equals(type)) {
                return objectMapper.convertValue(map, InventoryCommands.ReserveInventory.class);
            } else if ("ReleaseInventory".equals(type)) {
                return objectMapper.convertValue(map, InventoryCommands.ReleaseInventory.class);
            }
            return map;
        } catch (Exception e) {
            log.error("LinkedHashMap을 커맨드로 변환 실패: type={}, error={}", type, e.getMessage(), e);
            return map;
        }
    }
    
    private Object convertLinkedHashMapToPaymentEvent(Map<String, Object> payload, String eventType) {
        try {
            switch (eventType) {
                case "PaymentCreated":
                    return objectMapper.convertValue(payload, PaymentEvents.PaymentCreated.class);
                case "PaymentAuthorized":
                    return objectMapper.convertValue(payload, PaymentEvents.PaymentAuthorized.class);
                case "PaymentFailed":
                    return objectMapper.convertValue(payload, PaymentEvents.PaymentFailed.class);
                case "PaymentCanceled":
                    return objectMapper.convertValue(payload, PaymentEvents.PaymentCanceled.class);
                default:
                    log.warn("알 수 없는 결제 이벤트 타입: {}", eventType);
                    return payload;
            }
        } catch (Exception e) {
            log.error("LinkedHashMap을 PaymentEvent로 변환 실패: eventType={}, error={}", eventType, e.getMessage(), e);
            return payload;
        }
    }
}
