package com.example.cloudfour.storeservice.domain.store.controller;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;


public class StoreCommonResponseDTO {
    @Getter
    @Builder
    public static class StoreCommonMainResponseDTO{
        private String phone;
        private String content;
        private Integer minPrice;
        private Integer deliveryTip;
        private String operationHours;
        private String closedDays;
        private String category;
    }
    @Getter
    @Builder
    public static class StoreCommonOptionResponseDTO{
        private float rating;
        private int reviewCount;
    }

    @Getter
    @Builder
    public static class StoreCommonsBaseResponseDTO{
        protected UUID storeId;
        protected String name;
        protected String address;
        protected String storePicture;
    }
}
