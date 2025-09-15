package com.example.cloudfour.cartservice.commondto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class StoreResponseDTO {
    private UUID storeId;
    private UUID userId;
    private String name;
}
