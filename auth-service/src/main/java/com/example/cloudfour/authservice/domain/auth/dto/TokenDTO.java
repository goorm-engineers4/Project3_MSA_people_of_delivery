package com.example.cloudfour.authservice.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenDTO {
    private final String grantType;
    private final String accessToken;
    private final String refreshToken;
    private final Long accessTokenExpiresIn;
}

