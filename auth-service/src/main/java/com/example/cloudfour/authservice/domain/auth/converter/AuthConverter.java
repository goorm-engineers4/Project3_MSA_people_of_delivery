package com.example.cloudfour.authservice.domain.auth.converter;

import com.example.cloudfour.authservice.domain.auth.dto.AuthModelDTO;
import com.example.cloudfour.authservice.domain.auth.dto.AuthResponseDTO;
import com.example.cloudfour.authservice.domain.auth.dto.TokenDTO;

public class AuthConverter {

    public static AuthResponseDTO.AuthRegisterResponseDTO toAuthRegisterResponseDTO(AuthModelDTO.RegisterResultDTO m) {
        return AuthResponseDTO.AuthRegisterResponseDTO.builder()
                .userId(m.userId())
                .email(m.email())
                .nickname(m.nickname())
                .role(m.role())
                .build();
    }

    public static AuthResponseDTO.AuthTokenResponseDTO toAuthTokenResponseDTO(TokenDTO token) {
        return AuthResponseDTO.AuthTokenResponseDTO.builder()
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .build();
    }
}


