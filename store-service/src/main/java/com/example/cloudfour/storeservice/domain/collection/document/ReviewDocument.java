package com.example.cloudfour.storeservice.domain.collection.document;

import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document("review-service")
public class ReviewDocument {
    @Id
    private String id;

    private UUID reviewId;
    private UUID userId;
    private UUID storeId;
    private String userName;
    private Float score;
    private String content;
    private String pictureUrl;
    private LocalDateTime createdAt;
}
