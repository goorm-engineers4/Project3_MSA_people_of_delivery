package com.example.cloudfour.modulecommon.idempotency;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "processed_messages",
       uniqueConstraints = @UniqueConstraint(name = "uk_consumer_topic_msg", columnNames = {"consumer_name", "topic", "msg_id"}),
       indexes = {
           @Index(name = "idx_processed_msg_consumer_topic_msg", columnList = "consumer_name,topic,msg_id"),
           @Index(name = "idx_processed_msg_consumer_msgid", columnList = "consumer_name,msg_id")
       })
public class ProcessedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "msg_id", nullable = false, length = 200)
    private String msgId;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
