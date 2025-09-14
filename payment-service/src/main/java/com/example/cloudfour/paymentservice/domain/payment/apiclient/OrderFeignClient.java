package com.example.cloudfour.paymentservice.domain.payment.apiclient;

import com.example.cloudfour.paymentservice.commondto.OrderResponseDTO;
import com.example.cloudfour.paymentservice.config.FeignConfig;
import com.example.cloudfour.paymentservice.domain.payment.dto.OrderStatusUpdateRequestDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(
        name = "cart-service",
        path = "/internal/orders",
        configuration = FeignConfig.class
)
@CircuitBreaker(name="order-circuit")
public interface OrderFeignClient {
    @GetMapping("/{orderId}")
    OrderResponseDTO getOrderById(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") UUID userId
    );

    @PatchMapping("/{orderId}/status")
    void updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestBody OrderStatusUpdateRequestDTO request
    );
}
