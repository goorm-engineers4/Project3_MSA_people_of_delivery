package com.example.cloudfour.authservice.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

public class AuthResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class AuthRegisterResponseDTO {
        private UUID userId;
        private String email;
        private String nickname;
        private String role;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AuthTokenResponseDTO {
        private String accessToken;
        private String refreshToken;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AuthRefreshTokenResponseDTO {
        private String accessToken;
        private Long accessTokenExpiresIn;
    }
}
