package com.example.cloudfour.storeservice.domain.menu.service;

import com.example.cloudfour.storeservice.domain.menu.dto.OrderEvent;
import com.example.cloudfour.storeservice.domain.menu.dto.PaymentEvent;
import com.example.cloudfour.storeservice.domain.menu.dto.StockResponseDTO;
import com.example.cloudfour.storeservice.domain.menu.service.command.StockCommandService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockKafkaService {

    private final RedisInventoryService redisInventoryService;
    private final StockCommandService stockCommandService;

    @KafkaListener(topics = "order-completed")
    public void startOrder(OrderEvent.OrderCompletedEvent event) {
        List<StockResponseDTO.StockReserveResponseDTO> reservations = new ArrayList<>();
        List<UUID> successfulReservations = new ArrayList<>();

        try {
            boolean allSuccess = true;

            for (OrderEvent.OrderCompletedEvent.OrderItem item : event.getOrderItems()) {
                StockResponseDTO.StockReserveResponseDTO result = redisInventoryService.reserveStock(
                        item.getMenuId(),
                        item.getQuantity(),
                        event.getOrderId()
                );

                reservations.add(result);

                if (result.isSuccess()) {
                    successfulReservations.add(item.getMenuId());
                } else {
                    allSuccess = false;
                    break;
                }
            }

            if (!allSuccess) {
                rollbackReservations(event.getOrderId());

                log.warn("주문 예약 실패 - OrderId: {}", event.getOrderId());
                return;
            }

            log.info("주문 예약 성공 - OrderId: {}", event.getOrderId());

        } catch (Exception e) {
            rollbackReservations(event.getOrderId());
            log.error("주문 예약 실패: {}", event.getOrderId(), e);
        }
    }


    @KafkaListener(topics = "payment-completed")
    public void handlePaymentCompleted(PaymentEvent.PaymentCompletedEvent event) {
        UUID orderId = event.getOrderId();
        List<PaymentEvent.PaymentCompletedEvent.OrderItem> item = event.getOrderItems();
        try {
            boolean success = stockCommandService.decreaseListStock(item);
            if (success) {
                redisInventoryService.releaseReservation(orderId);
                log.info("재고 차감 성공 - OrderId: {}", orderId);

            } else {
                log.warn("재고 부족으로 인해 차감 실패- OrderId: {}", orderId);
            }

        } catch (Exception e) {
            log.error("재고 차감 실패: {}", event.getOrderId(), e);
        }
    }

    @KafkaListener(topics = "payment-failed")
    public void handlePaymentFailed(PaymentEvent.PaymentFailedEvent event) {
        redisInventoryService.releaseReservation(event.getOrderId());
        log.info("오류로 인해 예약 해제- OrderId: {}",
                event.getOrderId());
    }

    private void rollbackReservations(UUID orderId) {
        redisInventoryService.releaseReservation(orderId);
    }
}
