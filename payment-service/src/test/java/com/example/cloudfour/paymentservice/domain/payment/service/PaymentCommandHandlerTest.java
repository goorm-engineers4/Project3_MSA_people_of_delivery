package com.example.cloudfour.paymentservice.domain.payment.service;

import com.example.cloudfour.modulecommon.idempotency.MessageIdempotencyService;
import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.example.cloudfour.modulecommon.messaging.MessageConsumer;
import com.example.cloudfour.modulecommon.messaging.MsgMeta;
import com.example.cloudfour.modulecommon.messaging.payment.PaymentCommands;
import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.example.cloudfour.modulecommon.schedule.ScheduledTaskService;
import com.example.cloudfour.paymentservice.domain.payment.entity.Payment;
import com.example.cloudfour.paymentservice.domain.payment.enums.PaymentStatus;
import com.example.cloudfour.paymentservice.domain.payment.repository.PaymentRepository;
import com.example.cloudfour.paymentservice.domain.payment.service.command.PaymentCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.LinkedHashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCommandHandler 단위테스트")
class PaymentCommandHandlerTest {

    @Mock private OutboxService outboxService;
    @Mock private MessageConsumer messageConsumer;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentCommandService paymentCommandService;
    @Mock private ObjectMapper objectMapper;
    @Mock private ScheduledTaskService scheduledTaskService;
    @Mock private MessageIdempotencyService idempotencyService;
    @Mock private Acknowledgment acknowledgment;

    @InjectMocks
    private PaymentCommandHandler handler;

    private String topic;
    private int partition;
    private long offset;
    private String key;
    private MsgMeta meta;

    private UUID orderId;
    private UUID userId;
    private UUID storeId;
    private String paymentKey;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(handler, "paymentEventsTopic", "payment.events.v1");
        topic = "payment.commands.v1";
        partition = 0;
        offset = 10L;
        key = "k1";
        meta = MsgMeta.builder()
                .msgId(UUID.randomUUID().toString())
                .sagaId(UUID.randomUUID().toString())
                .type("CreatePayment")
                .version("v1")
                .source("test")
                .causationId(null)
                .correlationId(null)
                .timestamp(java.time.Instant.now())
                .attempt(1)
                .build();

        orderId = UUID.randomUUID();
        userId = UUID.randomUUID();
        storeId = UUID.randomUUID();
        paymentKey = "toss_pk_" + UUID.randomUUID();

        when(idempotencyService.markIfNotProcessed(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("CreatePayment 처리: 서비스 호출 후 ack")
    void handlePaymentCommand_CreatePayment_Success() {
        PaymentCommands.CreatePayment cmd = PaymentCommands.CreatePayment.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .amount(12000)
                .paymentMethod("CARD")
                .build();
        Envelope<PaymentCommands.CreatePayment> env = Envelope.<PaymentCommands.CreatePayment>builder()
                .meta(meta)
                .payload(cmd)
                .build();

        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);

        verify(messageConsumer).logMessageReceived(eq(env), eq(topic), eq(partition), eq(offset), eq(key));
        verify(paymentCommandService).createPayment(orderId, userId, storeId, 12000, "CARD");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("멱등성 중복 메시지: 바로 ack")
    void handlePaymentCommand_Duplicated_AckAndSkip() {
        when(idempotencyService.markIfNotProcessed(anyString(), anyString(), anyString())).thenReturn(false);

        Envelope<PaymentCommands.CreatePayment> env = Envelope.<PaymentCommands.CreatePayment>builder()
                .meta(meta)
                .payload(PaymentCommands.CreatePayment.builder().orderId(orderId).userId(userId).storeId(storeId).amount(1).paymentMethod("CARD").build())
                .build();

        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(paymentCommandService, never()).createPayment(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("LinkedHashMap payload도 처리")
    void handlePaymentCommand_LinkedHashMapPayload_Success() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("orderId", orderId.toString());
        map.put("userId", userId.toString());
        map.put("storeId", storeId.toString());
        map.put("amount", 5000);
        map.put("paymentMethod", "CARD");

        PaymentCommands.CreatePayment converted = PaymentCommands.CreatePayment.builder()
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .amount(5000)
                .paymentMethod("CARD")
                .build();

        Envelope<Object> env = Envelope.builder().meta(meta).payload(map).build();

        when(objectMapper.convertValue(map, PaymentCommands.CreatePayment.class)).thenReturn(converted);

        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);

        verify(paymentCommandService).createPayment(orderId, userId, storeId, 5000, "CARD");
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("CancelPayment 처리: 성공 시 이벤트 발행 및 ack")
    void handlePaymentCommand_CancelPayment_Success() {
        PaymentCommands.CancelPayment cmd = PaymentCommands.CancelPayment.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentKey(paymentKey)
                .reason("사유")
                .build();
        Envelope<PaymentCommands.CancelPayment> env = Envelope.<PaymentCommands.CancelPayment>builder()
                .meta(meta)
                .payload(cmd)
                .build();

        Payment payment = Payment.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .userId(userId)
                .storeId(storeId)
                .amount(1000)
                .paymentMethod("CARD")
                .paymentStatus(PaymentStatus.APPROVED)
                .build();

        when(paymentRepository.findByPaymentKeyAndOrderId(paymentKey, orderId)).thenReturn(java.util.Optional.of(payment));

        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);

        verify(paymentCommandService).cancelPayment(any(), eq(orderId), eq(userId));
        verify(outboxService).saveEvent(eq(orderId.toString()), eq("Payment"), eq("PaymentCanceled"), any(), anyString(), eq(orderId.toString()));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("CancelPayment: 결제 없음이어도 실패 이벤트 발행 및 ack")
    void handlePaymentCommand_CancelPayment_PaymentNotFound_StillPublishesEvent() {
        PaymentCommands.CancelPayment cmd = PaymentCommands.CancelPayment.builder()
                .orderId(orderId)
                .userId(userId)
                .paymentKey(paymentKey)
                .reason("사유")
                .build();
        Envelope<PaymentCommands.CancelPayment> env = Envelope.<PaymentCommands.CancelPayment>builder()
                .meta(meta)
                .payload(cmd)
                .build();

        when(paymentRepository.findByPaymentKeyAndOrderId(paymentKey, orderId)).thenReturn(java.util.Optional.empty());

        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);

        verify(outboxService).saveEvent(eq(orderId.toString()), eq("Payment"), eq("PaymentCanceled"), any(), anyString(), eq(orderId.toString()));
        verify(acknowledgment).acknowledge();
    }

    @Test
    @DisplayName("알 수 없는 payload 타입: ack")
    void handlePaymentCommand_UnknownPayload_Ack() {
        Envelope<String> env = Envelope.<String>builder().meta(meta).payload("unknown").build();
        handler.handlePaymentCommand(env, topic, partition, offset, key, acknowledgment);
        verify(acknowledgment).acknowledge();
    }
}
