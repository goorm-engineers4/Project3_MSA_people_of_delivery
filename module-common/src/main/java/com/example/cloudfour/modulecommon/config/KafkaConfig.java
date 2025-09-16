package com.example.cloudfour.modulecommon.config;

import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.DefaultErrorHandler;
// import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaders;
import java.nio.charset.StandardCharsets;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;

import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    public KafkaConfig(KafkaProperties kafkaProperties, ObjectMapper objectMapper) {
        this.kafkaProperties = kafkaProperties;
        this.objectMapper = objectMapper;
    }

    @Bean("envelopeProducerFactory")
    public ProducerFactory<String, Envelope<?>> envelopeProducerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean("envelopeKafkaTemplate")
    public KafkaTemplate<String, Envelope<?>> envelopeKafkaTemplate() {
        return new KafkaTemplate<>(envelopeProducerFactory());
    }

    @Bean("objectProducerFactory")
    public ProducerFactory<String, Object> objectProducerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildProducerProperties(null);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
            JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(objectProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = kafkaProperties.buildConsumerProperties(null);

        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            ErrorHandlingDeserializer.class);

        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
            JsonDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS,
            org.apache.kafka.common.serialization.StringDeserializer.class);
        
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, 
            "com.example.cloudfour.modulecommon.messaging.Envelope");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        factory.setCommonErrorHandler(mainErrorHandler());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, Object> dlqConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties(null);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, Object.class.getName());
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean("dlqKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> dlqKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setSyncCommits(true);
        factory.setCommonErrorHandler(dlqErrorHandler());
        return factory;
    }

    @Autowired(required = false)
    private DlqTopicResolver dlqTopicResolver;

    @Bean
    public DefaultErrorHandler mainErrorHandler() {
        ConsumerRecordRecoverer recoverer = (record, ex) -> {
            String original = record.topic();
            String dlq = (dlqTopicResolver != null) ? dlqTopicResolver.resolve(original) : defaultDlq(original);

            @SuppressWarnings("unchecked")
            ProducerRecord<String, Object> out = new ProducerRecord<>(dlq,
                    record.key() == null ? null : record.key().toString(),
                    record.value());

            Headers headers = out.headers();
            record.headers().forEach(h -> headers.add(h));

            Integer attempt = null;
            try {
                var hdr = record.headers().lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
                if (hdr != null) {
                    String s = new String(hdr.value(), StandardCharsets.UTF_8);
                    attempt = Integer.parseInt(s);
                }
            } catch (Exception ignore) { }

            if (attempt != null) {
                headers.add("x-original-delivery-attempt",
                        Integer.toString(attempt).getBytes(StandardCharsets.UTF_8));
            }

            kafkaTemplate().send(out);
        };
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
        handler.setCommitRecovered(true);
        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            try {
                var headers = record.headers();
                headers.remove("x-original-delivery-attempt");
                headers.add("x-original-delivery-attempt",
                        Integer.toString(deliveryAttempt).getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignore) {}
        });
        return handler;
    }

    @Bean
    public DefaultErrorHandler dlqErrorHandler() {
        ConsumerRecordRecoverer noOp = (record, ex) -> {
        };
        return new DefaultErrorHandler(noOp, new FixedBackOff(0L, 0));
    }

    private String defaultDlq(String topic) {
        if (topic.endsWith(".v1")) return topic.substring(0, topic.length() - 3) + ".dlq.v1";
        return topic + ".dlq";
    }
}
