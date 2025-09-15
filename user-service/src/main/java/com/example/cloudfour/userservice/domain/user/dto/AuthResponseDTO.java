package com.example.cloudfour.userservice.domain.user.dto;

import java.util.UUID;

public class AuthResponseDTO {
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
