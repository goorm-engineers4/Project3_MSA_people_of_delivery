package com.example.cloudfour.authservice.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshDTO {
    private final String accessToken;
    private final Long accessTokenExpiresIn;
}
