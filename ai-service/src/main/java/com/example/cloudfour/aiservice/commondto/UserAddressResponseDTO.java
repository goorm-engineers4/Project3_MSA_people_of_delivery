package com.example.cloudfour.aiservice.commondto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserAddressResponseDTO {
    private UUID addressId;
    private String address;
    private UUID regionId;
    private String si;
    private String gu;
    private String dong;
}