package com.example.cloudfour.userservice.domain.user.dto;

import com.example.cloudfour.userservice.domain.user.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

public class UserResponseDTO {

    @Getter
    @Builder
    public static class MeResponseDTO {
        UUID userId;
        String email;
        String nickname;
        String number;
        Role role;
    }

    @Getter
    @Builder
    public static class AddressResponseDTO {
        UUID addressId;
        String address;
        UUID regionId;
    }

    @Getter
    @Builder
    public static class AddressListResponseDTO {
        List<AddressResponseDTO> addresses;
    }
}