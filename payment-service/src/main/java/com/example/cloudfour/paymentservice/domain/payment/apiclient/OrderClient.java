package com.example.cloudfour.paymentservice.domain.payment.apiclient;

import com.example.cloudfour.paymentservice.commondto.OrderResponseDTO;
// import com.example.cloudfour.paymentservice.domain.payment.dto.OrderStatusUpdateRequestDTO; // 이벤트로 처리됨
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderClient {

    private final OrderFeignClient orderClient;

    public OrderResponseDTO getOrderById(String orderId, UUID userId) {
        try {
            log.info("주문 정보 조회 요청: orderId={}, userId={}", orderId, userId);

            OrderResponseDTO order = orderClient.getOrderById(orderId, userId);
            log.info("주문 정보 조회 성공: orderId={}, userId={}", orderId, userId);
            
            return order;
        } catch (Exception e) {
            log.error("주문 정보 조회 실패: orderId={}, userId={}, error={}", orderId, userId, e.getMessage());
            throw new RuntimeException("주문 정보 조회에 실패했습니다: " + e.getMessage());
        }
    }

    // 이벤트로 처리됨: Saga에서 OrderApproved/OrderCanceled 이벤트 발행
    // public void updateOrderStatus(String orderId, String newStatus) {
    //     try {
    //
    //         log.info("주문 상태 업데이트 요청: orderId={}, newStatus={}", orderId, newStatus);
    //
    //         OrderStatusUpdateRequestDTO request = OrderStatusUpdateRequestDTO
    //                 .builder().status(newStatus).build();
    //
    //         orderClient.updateOrderStatus(orderId, request);
    //
    //         log.info("주문 상태 업데이트 성공: orderId={}, newStatus={}", orderId, newStatus);
    //
    //     } catch (Exception e) {
    //         log.error("주문 상태 업데이트 실패: orderId={}, newStatus={}, error={}", orderId, newStatus, e.getMessage());
    //         log.warn("주문 상태 업데이트 실패했지만 결제 처리는 계속 진행합니다.");
    //     }
    // }
}

