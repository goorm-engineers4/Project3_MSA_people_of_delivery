package com.example.cloudfour.storeservice.domain.menu.service.event;

import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.example.cloudfour.modulecommon.messaging.MessageConsumer;
import com.example.cloudfour.modulecommon.messaging.inventory.InventoryEvents;
import com.example.cloudfour.storeservice.domain.menu.service.RedisInventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventHandler {

    private final RedisInventoryService redisInventoryService;
    private final MessageConsumer messageConsumer;
    private final ObjectMapper objectMapper;
    private final com.example.cloudfour.modulecommon.outbox.service.OutboxService outboxService;
    private final com.example.cloudfour.modulecommon.idempotency.MessageIdempotencyService idempotencyService;

    @org.springframework.beans.factory.annotation.Value("${kafka.topics.inventoryEvents:inventory.events.v1}")
    private String inventoryEventsTopic;

    @KafkaListener(topics = "${kafka.topics.inventoryEvents:inventory.events.v1}",
            groupId = "inventory-event-handler")
    public void handleInventoryEvents(
            @Payload Envelope<Object> envelope,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            if (envelope == null || envelope.getMeta() == null) {
                log.warn("유효하지 않은 메시지(envelope/meta null) 수신: topic={}, key={}", topic, key);
                acknowledgment.acknowledge();
                return;
            }
            messageConsumer.logMessageReceived(envelope, topic, partition, offset, key);
            if (!idempotencyService.markIfNotProcessed("inventory-event-handler", envelope.getMeta().getMsgId(), topic)) {
                log.warn("중복 이벤트 스킵: consumer=inventory-event-handler, msgId={}", envelope.getMeta().getMsgId());
                acknowledgment.acknowledge();
                return;
            }

            String type = envelope.getMeta().getType();
            Object payload = envelope.getPayload();

            if ("InventoryReleased".equals(type)) {
                InventoryEvents.InventoryReleased event;
                if (payload instanceof LinkedHashMap) {
                    event = objectMapper.convertValue(payload, InventoryEvents.InventoryReleased.class);
                } else {
                    event = (InventoryEvents.InventoryReleased) payload;
                }

                log.info("인벤토리 해제 처리 시작(이벤트 기반): orderId={}", event.getOrderId());
                try {
                    redisInventoryService.releaseReservation(event.getOrderId());
                    log.info("인벤토리 해제 처리 완료(이벤트 기반): orderId={}", event.getOrderId());

                    var completed = com.example.cloudfour.storeservice.domain.menu.converter.InventoryEventConverter
                            .createInventoryReleaseCompletedEvent(event.getOrderId(), event.getStoreId(), true, null);
                    outboxService.saveEvent(
                            event.getOrderId().toString(),
                            "Inventory",
                            "InventoryReleaseCompleted",
                            completed,
                            inventoryEventsTopic,
                            event.getOrderId().toString()
                    );
                } catch (Exception ex) {
                    log.error("인벤토리 해제 처리 실패: orderId={}, error={}", event.getOrderId(), ex.getMessage(), ex);
                    var completed = com.example.cloudfour.storeservice.domain.menu.converter.InventoryEventConverter
                            .createInventoryReleaseCompletedEvent(event.getOrderId(), event.getStoreId(), false, ex.getMessage());
                    outboxService.saveEvent(
                            event.getOrderId().toString(),
                            "Inventory",
                            "InventoryReleaseCompleted",
                            completed,
                            inventoryEventsTopic,
                            event.getOrderId().toString()
                    );
                }
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("인벤토리 이벤트 처리 실패: error={}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
