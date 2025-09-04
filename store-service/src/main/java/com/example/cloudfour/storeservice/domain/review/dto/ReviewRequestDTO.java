package com.example.cloudfour.storeservice.domain.review.dto;

import com.example.cloudfour.storeservice.domain.review.controller.ReviewCommonRequestDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;


public class ReviewRequestDTO {
    @Getter
    @Builder
    public static class ReviewCreateRequestDTO{
        UUID storeId;
        @JsonUnwrapped
        ReviewCommonRequestDTO reviewCommonRequestDTO;
    }

    @Getter
    @Builder
    public static class ReviewUpdateRequestDTO{
        @JsonUnwrapped
        ReviewCommonRequestDTO reviewCommonRequestDTO;
    }
}
