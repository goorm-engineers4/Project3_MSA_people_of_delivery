package com.example.cloudfour.authservice.client;

import com.example.cloudfour.authservice.domain.auth.dto.UserRequestDTO;
import com.example.cloudfour.authservice.domain.auth.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserClient {
    private final UserFeignClient feign;

    public UserResponseDTO.ExistsByEmailResponseDTO existsByEmail(String email) {
        return feign.existsByEmail(email);
    }

    public UserResponseDTO.UserBriefResponseDTO create(UserRequestDTO.CreateUserRequestDTO req) {
        return feign.create(req);
    }

    public UserResponseDTO.UserBriefResponseDTO byEmail(String email) {
        return feign.byEmail(email);
    }

    // 오류
    public UserResponseDTO.UserBriefResponseDTO byId(UUID id) {
        return feign.byId(id);
    }

    public UserResponseDTO.PasswordVerifyResponseDTO verifyPassword(UUID id, String rawPassword) {
        return feign.verifyPassword(id, new UserRequestDTO.PasswordVerifyRequestDTO(rawPassword));
    }

    public void markEmailVerified(UUID id) {
        feign.markEmailVerified(id);
    }

    public void changePassword(UUID id, String current, String next) {
        feign.changePassword(id, new UserRequestDTO.ChangePasswordRequestDTO(current, next));
    }

    public boolean existsByEmailBool(String email) {
        var res = existsByEmail(email);
        return res != null && res.exists();
    }

    public void startEmailChange(UUID id, String newEmail) {
        feign.startEmailChange(id, new UserRequestDTO.EmailChangeStartRequestDTO(newEmail));
    }

    public void confirmEmailChange(UUID id, String newEmail) {
        feign.confirmEmailChange(id, new UserRequestDTO.EmailChangeConfirmRequestDTO(newEmail));
    }
}
