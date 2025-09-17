package com.example.cloudfour.paymentservice.domain.payment.service.event;

import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.example.cloudfour.paymentservice.domain.payment.entity.Payment;
import com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentErrorCode;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentEventService 단위테스트")
class PaymentEventServiceTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private PaymentEventService paymentEventService;

    private Payment payment;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentEventService, "paymentEventsTopic", "payment.events.v1");

        payment = Payment.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .storeId(UUID.randomUUID())
                .amount(12000)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("PaymentCreated 이벤트 발행 성공")
    void publishPaymentCreated_Success() {
        paymentEventService.publishPaymentCreated(payment);

        verify(outboxService).saveEvent(
                eq(payment.getOrderId().toString()),
                eq("Payment"),
                eq("PaymentCreated"),
                any(),
                eq("payment.events.v1"),
                eq(payment.getOrderId().toString())
        );
    }

    @Test
    @DisplayName("PaymentCreated 이벤트 발행 실패 시 예외 발생")
    void publishPaymentCreated_Failure_Throws() {
        doThrow(new RuntimeException("outbox fail")).when(outboxService)
                .saveEvent(anyString(), anyString(), anyString(), any(), anyString(), anyString());

        assertThatThrownBy(() -> paymentEventService.publishPaymentCreated(payment))
                .isInstanceOf(PaymentException.class)
                .hasFieldOrPropertyWithValue("code", PaymentErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("PaymentCancelled 이벤트 발행 성공")
    void publishPaymentCancelled_Success() {
        paymentEventService.publishPaymentCancelled(payment, "고객 요청");

        verify(outboxService).saveEvent(
                eq(payment.getOrderId().toString()),
                eq("Payment"),
                eq("PaymentCancelled"),
                any(),
                eq("payment.events.v1"),
                eq(payment.getOrderId().toString())
        );
    }

    @Test
    @DisplayName("PaymentCancelled 이벤트 발행 실패 시 예외 발생")
    void publishPaymentCancelled_Failure_Throws() {
        doThrow(new RuntimeException("outbox fail")).when(outboxService)
                .saveEvent(anyString(), anyString(), anyString(), any(), anyString(), anyString());

        assertThatThrownBy(() -> paymentEventService.publishPaymentCancelled(payment, "사유"))
                .isInstanceOf(PaymentException.class)
                .hasFieldOrPropertyWithValue("code", PaymentErrorCode.INTERNAL_SERVER_ERROR);
    }
}

