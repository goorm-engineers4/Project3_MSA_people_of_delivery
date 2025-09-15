package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantDescriptionRequestDTO {
    private StoreDTO store;

    @Getter
    @Builder
    public static class StoreDTO {
        private String name;
        private String address;
        private String storePicture;
        private String phone;
        private String content;
        private Integer minPrice;
        private Integer deliveryTip;
        private Float rating;
        private Integer likeCount;
        private Integer reviewCount;
        private String operationHours;
        private String closedDays;
        private String storeCategory;
    }
}