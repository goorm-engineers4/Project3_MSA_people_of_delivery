package com.example.cloudfour.authservice.domain.auth.dto;

import java.util.UUID;

public class AuthModelDTO {

    public record RegisterResultDTO(
            UUID userId,
            String email,
            String nickname,
            String role
    ) {}
}
