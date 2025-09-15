package com.example.cloudfour.userservice.domain.user.dto;

public class AuthRequestDTO {
    public record CreateUserRequestDTO(
            String email,
            String nickname,
            String number,
            String rawPassword,
            String role
    ) {}

    public record PasswordVerifyRequestDTO(String rawPassword) {}

    public record ChangePasswordRequestDTO(String currentPassword, String newPassword) {}

    public record EmailChangeStartRequestDTO(String newEmail) {}

    public record EmailChangeConfirmRequestDTO(String newEmail) {}
}
