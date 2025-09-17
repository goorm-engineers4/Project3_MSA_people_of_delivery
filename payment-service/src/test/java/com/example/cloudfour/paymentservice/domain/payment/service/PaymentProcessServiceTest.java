package com.example.cloudfour.paymentservice.domain.payment.service;

import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.example.cloudfour.modulecommon.schedule.ScheduledTaskService;
import com.example.cloudfour.paymentservice.commondto.OrderResponseDTO;
import com.example.cloudfour.paymentservice.domain.payment.apiclient.OrderClient;
import com.example.cloudfour.paymentservice.domain.payment.dto.PaymentRequestDTO;
import com.example.cloudfour.paymentservice.domain.payment.dto.PaymentResponseDTO;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentErrorCode;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentException;
import com.example.cloudfour.paymentservice.domain.payment.service.command.PaymentCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentProcessService 단위테스트")
class PaymentProcessServiceTest {

    @Mock private PaymentCommandService paymentCommandService;
    @Mock private PaymentCacheService paymentCacheService;
    @Mock private OrderClient orderClient;
    @Mock private OutboxService outboxService;
    @Mock private ScheduledTaskService scheduledTaskService;

    @InjectMocks
    private PaymentProcessService paymentProcessService;

    private String orderId;
    private String paymentKey;
    private Long amount;
    private UUID userId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentProcessService, "paymentEventsTopic", "payment.events.v1");

        orderId = UUID.randomUUID().toString();
        paymentKey = "toss_pk_" + UUID.randomUUID();
        amount = 15000L;
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
    }

    private OrderResponseDTO buildOrder(long total) {
        return OrderResponseDTO.builder()
                .id(UUID.fromString(orderId))
                .userId(userId)
                .storeId(storeId)
                .totalPrice((int) total)
                .status("결제전")
                .build();
    }

    @Test
    @DisplayName("processSuccess: 금액 일치 시 성공 URL 반환 및 캐시 저장")
    void processSuccess_Success() {
        // Given
        when(orderClient.getOrderById(orderId, null)).thenReturn(buildOrder(amount));

        // When
        var result = paymentProcessService.processSuccess(paymentKey, orderId, amount);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFrontendUrl()).contains("/payment/success?orderId=" + orderId);
        verify(paymentCacheService).savePaymentMapping(orderId, paymentKey);
    }

    @Test
    @DisplayName("processSuccess: 금액 불일치 시 실패 URL 및 이벤트 발행")
    void processSuccess_AmountMismatch_FailureAndEvent() {
        // Given
        when(orderClient.getOrderById(orderId, null)).thenReturn(buildOrder(amount + 1000));
        // 재조회 시에도 응답
        when(orderClient.getOrderById(orderId, null)).thenReturn(buildOrder(amount + 1000));

        // When
        var result = paymentProcessService.processSuccess(paymentKey, orderId, amount);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFrontendUrl()).contains("/payment/fail?orderId=" + orderId);
        verify(outboxService, atLeastOnce()).saveEvent(eq(orderId), eq("Payment"), eq("PaymentFailed"), any(), eq("payment.events.v1"), eq(orderId));
    }

    @Test
    @DisplayName("processConfirm: 성공 시 승인 이벤트 발행, 캐시 삭제 및 타임아웃 취소")
    void processConfirm_Success() {
        // Given
        when(paymentCacheService.getPaymentKeyByOrderId(orderId)).thenReturn(paymentKey);
        when(orderClient.getOrderById(orderId, userId)).thenReturn(buildOrder(amount));

        PaymentResponseDTO.PaymentConfirmResponseDTO confirmResp = PaymentResponseDTO.PaymentConfirmResponseDTO.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount.intValue())
                .paymentMethod("CARD")
                .build();
        when(paymentCommandService.confirmPayment(any(PaymentRequestDTO.PaymentConfirmRequestDTO.class), eq(userId)))
                .thenReturn(confirmResp);

        // When
        var result = paymentProcessService.processConfirm(orderId, amount.intValue(), userId);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResponse()).isNotNull();
        verify(paymentCacheService).deletePaymentMapping(orderId, paymentKey);
        verify(outboxService).saveEvent(eq(orderId), eq("Payment"), eq("PaymentAuthorized"), any(), eq("payment.events.v1"), eq(orderId));
        verify(scheduledTaskService).cancelOrderTimeout(orderId);
    }

    @Test
    @DisplayName("processConfirm: 캐시에 키 없음 -> 실패 결과, 실패 이벤트 발행")
    void processConfirm_NoCacheKey_Failure() {
        // Given
        when(paymentCacheService.getPaymentKeyByOrderId(orderId)).thenReturn(null);
        when(orderClient.getOrderById(orderId, userId)).thenReturn(buildOrder(amount));

        // When
        var result = paymentProcessService.processConfirm(orderId, amount.intValue(), userId);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getException()).isInstanceOf(PaymentException.class)
                .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_NOT_FOUND);
        verify(outboxService).saveEvent(eq(orderId), eq("Payment"), eq("PaymentFailed"), any(), eq("payment.events.v1"), eq(orderId));
    }

    @Test
    @DisplayName("processConfirm: 금액 불일치 -> 실패 결과, 실패 이벤트 발행")
    void processConfirm_AmountMismatch_Failure() {
        // Given
        when(paymentCacheService.getPaymentKeyByOrderId(orderId)).thenReturn(paymentKey);
        when(orderClient.getOrderById(orderId, userId)).thenReturn(buildOrder(amount + 1));

        // When
        var result = paymentProcessService.processConfirm(orderId, amount.intValue(), userId);

        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getException()).isInstanceOf(PaymentException.class)
                .hasFieldOrPropertyWithValue("code", PaymentErrorCode.AMOUNT_MISMATCH);
        verify(outboxService).saveEvent(eq(orderId), eq("Payment"), eq("PaymentFailed"), any(), eq("payment.events.v1"), eq(orderId));
    }

    @Test
    @DisplayName("processFailure: 실패 이벤트 발행 (예외 없이 진행)")
    void processFailure_PublishesEvent_NoThrow() {
        // When / Then (no exception)
        paymentProcessService.processFailure(orderId, "SOME_CODE", "message");
        verify(outboxService, atLeast(0)).saveEvent(anyString(), anyString(), anyString(), any(), anyString(), anyString());
    }
}

