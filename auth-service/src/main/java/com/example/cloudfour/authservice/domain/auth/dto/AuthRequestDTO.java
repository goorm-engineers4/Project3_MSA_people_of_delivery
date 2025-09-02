package com.example.cloudfour.authservice.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequestDTO {

    public record RegisterRequestDTO(
            @Email @NotBlank String email,
            @NotBlank @Size(min=2, max=20) String nickname,
            @NotBlank @Size(min=8, max=64) String password,
            @NotBlank String role,
            @NotBlank String number
    ) {}

    public record LoginRequestDTO(
            @Email @NotBlank String email,
            @NotBlank @Size(min=8, max=64) String password
    ) {}

    public record RefreshTokenRequestDTO(
            @Email @NotBlank String email,
            @NotBlank String refreshToken
    ) {}

    public record PasswordChangeDto(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 64) String newPassword
    ) {}

    public record EmailCodeRequestDTO(
            @Email @NotBlank String email
    ) {}

    public record EmailVerifyRequestDTO(
            @Email @NotBlank String email,
            @NotBlank @Size(min=6, max=6) String code
    ) {}

    public record EmailChangeStartRequestDTO(
            @Email @NotBlank String newEmail
    ) {}

    public record EmailChangeVerifyRequestDTO(
            @Email @NotBlank String newEmail,
            @NotBlank @Size(min=6, max=6) String code
    ) {}
}

