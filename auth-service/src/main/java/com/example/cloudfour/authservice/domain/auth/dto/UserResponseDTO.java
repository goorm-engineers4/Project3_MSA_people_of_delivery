package com.example.cloudfour.authservice.domain.auth.dto;

import java.util.UUID;

public class UserResponseDTO {
    public record UserBriefResponseDTO(
            UUID id,
            String email,
            String role,
            String name,
            boolean emailVerified
    ) {}

    public record PasswordVerifyResponseDTO(boolean match) {}

    public record ExistsByEmailResponseDTO(boolean exists) {}
}
