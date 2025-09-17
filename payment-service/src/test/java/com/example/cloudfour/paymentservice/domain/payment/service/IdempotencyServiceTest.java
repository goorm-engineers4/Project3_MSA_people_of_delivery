package com.example.cloudfour.paymentservice.domain.payment.service;

import com.example.cloudfour.paymentservice.domain.payment.entity.Payment;
import com.example.cloudfour.paymentservice.domain.payment.entity.PaymentHistory;
import com.example.cloudfour.paymentservice.domain.payment.repository.PaymentHistoryRepository;
import com.example.cloudfour.paymentservice.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyService 단위테스트")
class IdempotencyServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentHistoryRepository paymentHistoryRepository;

    @InjectMocks
    private IdempotencyService idempotencyService;

    private Payment payment;
    private PaymentHistory history;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .orderId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .storeId(UUID.randomUUID())
                .amount(1000)
                .paymentMethod("CARD")
                .paymentStatus(com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus.PENDING)
                .build();

        history = PaymentHistory.builder()
                .payment(payment)
                .previousStatus(com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus.PENDING)
                .currentStatus(com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus.APPROVED)
                .build();
    }

    @Test
    @DisplayName("멱등키 생성은 UUID 문자열 반환")
    void generateIdempotencyKey_ReturnsUuidString() {
        String key = idempotencyService.generateIdempotencyKey();
        assertThat(key).isNotBlank();
        assertThat(key).contains("-");
    }

    @Test
    @DisplayName("승인 멱등성 체크는 repository 위임")
    void checkPaymentApprovalIdempotency_DelegatesToRepository() {
        when(paymentRepository.findByPaymentKey(anyString())).thenReturn(Optional.of(payment));

        Optional<Payment> result = idempotencyService.checkPaymentApprovalIdempotency("pk");

        assertThat(result).isPresent();
        verify(paymentRepository).findByPaymentKey("pk");
    }

    @Test
    @DisplayName("취소/웹훅 멱등성 체크는 history repository 위임")
    void checkCancelAndWebhookIdempotency_DelegatesToHistoryRepository() {
        when(paymentHistoryRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.of(history));

        assertThat(idempotencyService.checkPaymentCancelIdempotency("idem")).isPresent();
        assertThat(idempotencyService.checkWebhookIdempotency("idem")).isPresent();
        
        verify(paymentHistoryRepository, times(2)).findByIdempotencyKey("idem");
    }

    @Test
    @DisplayName("승인/취소/웹훅 멱등키 설정 시 엔티티에 키 세팅")
    void setIdempotencyKey_SetsValueOnEntities() {
        String k1 = idempotencyService.setPaymentApprovalIdempotency(payment);
        String k2 = idempotencyService.setPaymentCancelIdempotency(history);
        String k3 = idempotencyService.setWebhookIdempotency(history);

        assertThat(k1).isNotBlank();
        assertThat(k2).isNotBlank();
        assertThat(k3).isNotBlank();
    }

    @Test
    @DisplayName("멱등키 유효기간: 24시간 이내 true, 이후 false, null은 false")
    void isIdempotencyKeyValid_Behavior() {
        Instant now = Instant.now();

        assertThat(idempotencyService.isIdempotencyKeyValid(now.minus(23, ChronoUnit.HOURS))).isTrue();
        assertThat(idempotencyService.isIdempotencyKeyValid(now.minus(25, ChronoUnit.HOURS))).isFalse();
        assertThat(idempotencyService.isIdempotencyKeyValid(null)).isFalse();
    }
}

