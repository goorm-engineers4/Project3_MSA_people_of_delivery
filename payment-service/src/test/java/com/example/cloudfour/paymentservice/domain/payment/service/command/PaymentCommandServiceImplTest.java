package com.example.cloudfour.paymentservice.domain.payment.service.command;

import com.example.cloudfour.paymentservice.commondto.OrderResponseDTO;
import com.example.cloudfour.paymentservice.domain.payment.apiclient.OrderClient;
import com.example.cloudfour.paymentservice.domain.payment.apiclient.TossApiClient;
import com.example.cloudfour.paymentservice.domain.payment.apiclient.UserClient;
import com.example.cloudfour.paymentservice.domain.payment.converter.PaymentConverter;
import com.example.cloudfour.paymentservice.domain.payment.dto.PaymentRequestDTO;
import com.example.cloudfour.paymentservice.domain.payment.dto.PaymentResponseDTO;
import com.example.cloudfour.paymentservice.domain.payment.entity.Payment;
import com.example.cloudfour.paymentservice.domain.payment.entity.PaymentHistory;
import com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentErrorCode;
import com.example.cloudfour.paymentservice.domain.payment.exception.PaymentException;
import com.example.cloudfour.paymentservice.domain.payment.repository.PaymentHistoryRepository;
import com.example.cloudfour.paymentservice.domain.payment.repository.PaymentRepository;
import com.example.cloudfour.paymentservice.domain.payment.service.IdempotencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandServiceImpl 단위테스트")
class PaymentCommandServiceImplTest {

    @Mock private TossApiClient tossApiClient;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentHistoryRepository paymentHistoryRepository;
    @Mock private IdempotencyService idempotencyService;
    @Mock private PaymentConverter paymentConverter;
    @Mock private ObjectMapper objectMapper;
    @Mock private OrderClient orderClient;
    @Mock private UserClient userClient;

    @InjectMocks
    private PaymentCommandServiceImpl paymentCommandService;

    private UUID userId;
    private String orderId;
    private String paymentKey;
    private Integer amount;
    private TossApiClient.TossApproveResponse tossResponse;
    private PaymentRequestDTO.PaymentConfirmRequestDTO confirmRequest;
    private PaymentRequestDTO.PaymentCancelRequestDTO cancelRequest;
    private Payment samplePendingPayment;
    private Payment sampleApprovedPayment;
    private PaymentResponseDTO.PaymentConfirmResponseDTO confirmResponse;
    private PaymentResponseDTO.PaymentCancelResponseDTO cancelResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID().toString();
        paymentKey = "toss_payment_key_" + UUID.randomUUID();
        amount = 15000;

        tossResponse = new TossApiClient.TossApproveResponse();
        tossResponse.paymentKey = paymentKey;
        tossResponse.orderId = orderId;
        tossResponse.totalAmount = amount;
        tossResponse.method = "CARD";
        tossResponse.status = "DONE";
        tossResponse.approvedAt = LocalDateTime.now().toString();

        confirmRequest = PaymentRequestDTO.PaymentConfirmRequestDTO.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        cancelRequest = PaymentRequestDTO.PaymentCancelRequestDTO.builder()
                .cancelReason("고객 요청")
                .build();

        samplePendingPayment = Payment.builder()
                .paymentKey(null)
                .orderId(UUID.fromString(orderId))
                .userId(userId)
                .amount(amount)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        sampleApprovedPayment = Payment.builder()
                .paymentKey(paymentKey)
                .orderId(UUID.fromString(orderId))
                .userId(userId)
                .amount(amount)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .build();

        confirmResponse = PaymentResponseDTO.PaymentConfirmResponseDTO.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .approvedAtStr(LocalDateTime.now().toString())
                .build();

        cancelResponse = PaymentResponseDTO.PaymentCancelResponseDTO.builder()
                .paymentId(UUID.randomUUID())
                .paymentStatus(PaymentStatus.CANCELED)
                .cancelReason("고객 요청")
                .canceledAt(LocalDateTime.now())
                .build();

        lenient().when(userClient.existsUser(any(UUID.class))).thenReturn(true);

        OrderResponseDTO orderResponse = OrderResponseDTO.builder()
                .id(UUID.fromString(orderId))
                .userId(userId)
                .storeId(UUID.randomUUID())
                .totalPrice(amount)
                .status("결제전")
                .build();
        lenient().when(orderClient.getOrderById(anyString(), any(UUID.class))).thenReturn(orderResponse);
    }

    @Nested
    @DisplayName("결제 승인 (confirmPayment)")
    class ConfirmPaymentTests {
        @Test
        @DisplayName("정상적인 결제 승인 성공")
        void confirmPayment_Success() throws Exception {
            when(idempotencyService.checkPaymentApprovalIdempotency(anyString()))
                    .thenReturn(Optional.empty());
            when(idempotencyService.generateIdempotencyKey()).thenReturn("idem-123");
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(samplePendingPayment));
            when(tossApiClient.approvePayment(eq(paymentKey), eq(orderId), eq(amount), anyString()))
                    .thenReturn(tossResponse);
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenReturn(PaymentHistory.builder().build());
            when(paymentConverter.createPaymentApprovedHistory(any(Payment.class), anyString())).thenReturn(PaymentHistory.builder().build());
            when(paymentConverter.toConfirmResponse(any(Payment.class))).thenReturn(confirmResponse);

            PaymentResponseDTO.PaymentConfirmResponseDTO response = paymentCommandService.confirmPayment(confirmRequest, userId);

            assertThat(response).isNotNull();
            assertThat(response.getPaymentKey()).isEqualTo(paymentKey);
            assertThat(response.getOrderId()).isEqualTo(orderId);
            assertThat(response.getAmount()).isEqualTo(amount);
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);

            verify(idempotencyService).checkPaymentApprovalIdempotency(paymentKey);
            verify(paymentRepository).findByOrderIdAndUserId(UUID.fromString(orderId), userId);
            verify(tossApiClient).approvePayment(eq(paymentKey), eq(orderId), eq(amount), anyString());
            verify(paymentRepository).save(any(Payment.class));
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
            verify(paymentConverter).toConfirmResponse(any(Payment.class));
        }

        @Test
        @DisplayName("멱등성: 이미 처리된 결제 반환")
        void confirmPayment_DuplicateRequest_ReturnsExistingPayment() {
            when(idempotencyService.checkPaymentApprovalIdempotency(anyString()))
                    .thenReturn(Optional.of(sampleApprovedPayment));
            when(paymentConverter.toConfirmResponse(sampleApprovedPayment)).thenReturn(confirmResponse);

            PaymentResponseDTO.PaymentConfirmResponseDTO response = paymentCommandService.confirmPayment(confirmRequest, userId);

            assertThat(response).isNotNull();
            verify(tossApiClient, never()).approvePayment(anyString(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("이미 승인된 결제는 승인 재시도 없이 반환")
        void confirmPayment_AlreadyApproved_ReturnsExisting() {
            when(idempotencyService.checkPaymentApprovalIdempotency(anyString())).thenReturn(Optional.empty());
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(sampleApprovedPayment));
            when(paymentConverter.toConfirmResponse(sampleApprovedPayment)).thenReturn(confirmResponse);

            PaymentResponseDTO.PaymentConfirmResponseDTO response = paymentCommandService.confirmPayment(confirmRequest, userId);

            assertThat(response).isNotNull();
            verify(tossApiClient, never()).approvePayment(anyString(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("토스 API 호출 실패 시 예외 발생")
        void confirmPayment_TossApiCallFailed_ThrowsException() {
            when(idempotencyService.checkPaymentApprovalIdempotency(paymentKey)).thenReturn(Optional.empty());
            when(idempotencyService.generateIdempotencyKey()).thenReturn("idem-123");
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(samplePendingPayment));
            when(tossApiClient.approvePayment(anyString(), anyString(), anyInt(), anyString()))
                    .thenThrow(new RuntimeException("토스 API 호출 실패"));

            assertThatThrownBy(() -> paymentCommandService.confirmPayment(confirmRequest, userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_APPROVAL_FAILED);

            verify(idempotencyService).checkPaymentApprovalIdempotency(paymentKey);
            verify(tossApiClient).approvePayment(anyString(), anyString(), anyInt(), anyString());
        }

        @Test
        @DisplayName("ObjectMapper 직렬화 실패 시 예외 발생")
        void confirmPayment_ObjectMapperSerializationFailed_ThrowsException() throws Exception {
            when(idempotencyService.checkPaymentApprovalIdempotency(paymentKey)).thenReturn(Optional.empty());
            when(idempotencyService.generateIdempotencyKey()).thenReturn("idem-123");
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(samplePendingPayment));
            when(tossApiClient.approvePayment(anyString(), anyString(), anyInt(), anyString()))
                    .thenReturn(tossResponse);
            when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("직렬화 실패"));

            assertThatThrownBy(() -> paymentCommandService.confirmPayment(confirmRequest, userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_APPROVAL_FAILED);

            verify(idempotencyService).checkPaymentApprovalIdempotency(paymentKey);
            verify(tossApiClient).approvePayment(anyString(), anyString(), anyInt(), anyString());
        }

        @Test
        @DisplayName("결제 정보 없음")
        void confirmPayment_PaymentNotFound_ThrowsException() {
            when(idempotencyService.checkPaymentApprovalIdempotency(anyString())).thenReturn(Optional.empty());
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentCommandService.confirmPayment(confirmRequest, userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("사용자 없음")
        void confirmPayment_UserNotFound_ThrowsException() {
            when(userClient.existsUser(userId)).thenReturn(false);

            assertThatThrownBy(() -> paymentCommandService.confirmPayment(confirmRequest, userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("요청 금액/저장 금액 불일치")
        void confirmPayment_AmountMismatch_ThrowsException() {
            when(idempotencyService.checkPaymentApprovalIdempotency(anyString())).thenReturn(Optional.empty());
            Payment pending = Payment.builder()
                    .orderId(UUID.fromString(orderId))
                    .userId(userId)
                    .amount(amount + 1)
                    .paymentMethod("CARD")
                    .paymentStatus(PaymentStatus.PENDING)
                    .build();
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(pending));

            assertThatThrownBy(() -> paymentCommandService.confirmPayment(confirmRequest, userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.INVALID_INPUT);

            verify(tossApiClient, never()).approvePayment(anyString(), anyString(), anyInt(), anyString());
        }
    }

    @Nested
    @DisplayName("결제 취소 (cancelPayment)")
    class CancelPaymentTests {
        @Test
        @DisplayName("정상적인 결제 취소 성공")
        void cancelPayment_Success() {
            Payment payment = Payment.builder()
                    .paymentKey(paymentKey)
                    .orderId(UUID.fromString(orderId))
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("CARD")
                    .paymentStatus(PaymentStatus.APPROVED)
                    .build();

            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(payment));
            doNothing().when(tossApiClient).cancelPayment(paymentKey, "고객 요청");
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(paymentConverter.createPaymentCanceledHistory(any(Payment.class), anyString(), anyString()))
                    .thenReturn(PaymentHistory.builder().build());
            when(paymentHistoryRepository.save(any(PaymentHistory.class))).thenReturn(PaymentHistory.builder().build());
            when(paymentConverter.toCancelResponse(any(Payment.class), any(PaymentHistory.class))).thenReturn(cancelResponse);

            PaymentResponseDTO.PaymentCancelResponseDTO response = paymentCommandService.cancelPayment(cancelRequest, UUID.fromString(orderId), userId);

            assertThat(response).isNotNull();
            assertThat(response.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
            verify(tossApiClient).cancelPayment(paymentKey, "고객 요청");
            verify(paymentRepository).save(any(Payment.class));
            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }

        @Test
        @DisplayName("존재하지 않는 결제 취소 시 예외")
        void cancelPayment_PaymentNotFound_ThrowsException() {
            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentCommandService.cancelPayment(cancelRequest, UUID.fromString(orderId), userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 취소된 결제는 예외")
        void cancelPayment_AlreadyCanceled_ThrowsException() {
            Payment canceled = Payment.builder()
                    .paymentKey(paymentKey)
                    .orderId(UUID.fromString(orderId))
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("CARD")
                    .paymentStatus(PaymentStatus.CANCELED)
                    .canceledAt(LocalDateTime.now())
                    .build();

            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(canceled));

            assertThatThrownBy(() -> paymentCommandService.cancelPayment(cancelRequest, UUID.fromString(orderId), userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.INVALID_PAYMENT_STATUS);
        }

        @Test
        @DisplayName("토스 취소 실패 시 실패 히스토리 저장 후 예외")
        void cancelPayment_TossError_SavesFailedHistoryAndThrows() {
            Payment payment = Payment.builder()
                    .paymentKey(paymentKey)
                    .orderId(UUID.fromString(orderId))
                    .userId(userId)
                    .amount(amount)
                    .paymentMethod("CARD")
                    .paymentStatus(PaymentStatus.APPROVED)
                    .build();

            when(paymentRepository.findByOrderIdAndUserId(UUID.fromString(orderId), userId))
                    .thenReturn(Optional.of(payment));
            doThrow(new RuntimeException("toss cancel error")).when(tossApiClient).cancelPayment(anyString(), anyString());
            when(paymentConverter.createPaymentCancelFailedHistory(any(Payment.class), anyString()))
                    .thenReturn(PaymentHistory.builder().build());

            assertThatThrownBy(() -> paymentCommandService.cancelPayment(cancelRequest, UUID.fromString(orderId), userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.PAYMENT_CANCEL_FAILED);

            verify(paymentHistoryRepository).save(any(PaymentHistory.class));
        }

        @Test
        @DisplayName("사용자 없음")
        void cancelPayment_UserNotFound_ThrowsException() {
            when(userClient.existsUser(userId)).thenReturn(false);

            assertThatThrownBy(() -> paymentCommandService.cancelPayment(cancelRequest, UUID.fromString(orderId), userId))
                    .isInstanceOf(PaymentException.class)
                    .hasFieldOrPropertyWithValue("code", PaymentErrorCode.USER_NOT_FOUND);
        }
    }
}
