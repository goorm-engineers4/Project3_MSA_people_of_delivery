package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDescriptionResponseDTO {
    private boolean success;
    private String errorMessage;
    private String generatedDescription;
    private String marketingCopy;
    private String keyFeatures;
    private String suggestedTags;
}