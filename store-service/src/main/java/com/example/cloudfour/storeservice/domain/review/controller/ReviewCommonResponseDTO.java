package com.example.cloudfour.storeservice.domain.review.controller;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public class ReviewCommonResponseDTO {
    @Builder
    @Getter
    public static class ReviewCommonCrudResponseDTO{
        UUID userId;
        Float score;
        String content;
        String pictureUrl;
    }

    @Getter
    @Builder
    public static class ReviewCommonGetResponseDTO{
        UUID reviewId;
        Float score;
        String content;
        String pictureUrl;
    }
}
