package com.example.cloudfour.aiservice.commondto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StoreResponseDTO {
    private UUID storeId;
    private String name;
    private String description;
    private String category;
    private String address;
    private UUID regionId;
    private String si;
    private String gu;
    private String dong;
}
