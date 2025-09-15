package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDescriptionRequestDTO {
    private String name;
    private String productName;
    private String category;
    private String ingredients;
    private String price;
}