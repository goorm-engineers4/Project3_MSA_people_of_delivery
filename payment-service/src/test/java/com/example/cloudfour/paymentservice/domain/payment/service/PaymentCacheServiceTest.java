package com.example.cloudfour.paymentservice.domain.payment.service;

import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentErrorCode;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCacheService 단위테스트")
class PaymentCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private PaymentCacheService paymentCacheService;

    private String orderId;
    private String paymentKey;

    @BeforeEach
    void setUp() {
        orderId = java.util.UUID.randomUUID().toString();
        paymentKey = "toss_pk_" + java.util.UUID.randomUUID();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("결제 매핑 저장 성공")
    void savePaymentMapping_Success() {
        // When
        paymentCacheService.savePaymentMapping(orderId, paymentKey);

        // Then
        verify(valueOperations, times(2)).set(anyString(), any(), any());
    }

    @Test
    @DisplayName("결제 매핑 저장 실패 시 예외 발생")
    void savePaymentMapping_Failure_ThrowsException() {
        // Given
        doThrow(new RuntimeException("redis error")).when(valueOperations).set(anyString(), any(), any());

        // When & Then
        assertThatThrownBy(() -> paymentCacheService.savePaymentMapping(orderId, paymentKey))
                .isInstanceOf(PaymentException.class)
                .hasFieldOrPropertyWithValue("code", PaymentErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("orderId로 paymentKey 조회 성공")
    void getPaymentKeyByOrderId_Found() {
        // Given
        when(valueOperations.get(startsWith("payment:orderId:"))).thenReturn(paymentKey);

        // When
        String result = paymentCacheService.getPaymentKeyByOrderId(orderId);

        // Then
        assertThat(result).isEqualTo(paymentKey);
    }

    @Test
    @DisplayName("orderId로 paymentKey 조회 시 없음")
    void getPaymentKeyByOrderId_NotFound() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        String result = paymentCacheService.getPaymentKeyByOrderId(orderId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("orderId로 paymentKey 조회 실패 시 null")
    void getPaymentKeyByOrderId_Error_ReturnsNull() {
        // Given
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis error"));

        // When
        String result = paymentCacheService.getPaymentKeyByOrderId(orderId);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("paymentKey로 orderId 조회 성공")
    void getOrderIdByPaymentKey_Found() {
        // Given
        when(valueOperations.get(startsWith("payment:paymentKey:"))).thenReturn(orderId);

        // When
        String result = paymentCacheService.getOrderIdByPaymentKey(paymentKey);

        // Then
        assertThat(result).isEqualTo(orderId);
    }

    @Test
    @DisplayName("paymentKey로 orderId 조회 시 없음")
    void getOrderIdByPaymentKey_NotFound() {
        // Given
        when(valueOperations.get(anyString())).thenReturn(null);

        // When
        String result = paymentCacheService.getOrderIdByPaymentKey(paymentKey);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("paymentKey로 orderId 조회 실패 시 null")
    void getOrderIdByPaymentKey_Error_ReturnsNull() {
        // Given
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("redis error"));

        // When
        String result = paymentCacheService.getOrderIdByPaymentKey(paymentKey);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("결제 매핑 삭제 성공")
    void deletePaymentMapping_Success() {
        // When
        paymentCacheService.deletePaymentMapping(orderId, paymentKey);

        // Then
        verify(redisTemplate, times(2)).delete(anyString());
    }

    @Test
    @DisplayName("결제 매핑 삭제 실패해도 예외 없이 진행")
    void deletePaymentMapping_Error_NoThrow() {
        // Given
        doThrow(new RuntimeException("redis error")).when(redisTemplate).delete(anyString());

        // When / Then
        paymentCacheService.deletePaymentMapping(orderId, paymentKey);
    }
}
