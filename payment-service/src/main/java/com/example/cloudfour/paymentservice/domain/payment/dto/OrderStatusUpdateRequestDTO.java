package com.example.cloudfour.paymentservice.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderStatusUpdateRequestDTO {
    String status;
}
