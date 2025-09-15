package com.example.cloudfour.userservice.domain.user.service.query;

import com.example.cloudfour.userservice.domain.user.dto.AuthRequestDTO;
import com.example.cloudfour.userservice.domain.user.dto.AuthResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndIsDeletedFalse(email.toLowerCase(Locale.ROOT));
    }

    public AuthResponseDTO.UserBriefResponseDTO byEmail(String email) {
        var u = userRepository.findByEmailAndIsDeletedFalse(email.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UserException(UserErrorCode.EMAIL_NOT_FOUND));
        return toBrief(u);
    }

    public AuthResponseDTO.UserBriefResponseDTO byId(UUID id) {
        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        return toBrief(u);
    }

    public AuthResponseDTO.PasswordVerifyResponseDTO verifyPassword(UUID id, AuthRequestDTO.PasswordVerifyRequestDTO req) {
        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        boolean ok = passwordEncoder.matches(req.rawPassword(), u.getPassword());
        return new AuthResponseDTO.PasswordVerifyResponseDTO(ok);
    }

    private static AuthResponseDTO.UserBriefResponseDTO toBrief(User u) {
        return new AuthResponseDTO.UserBriefResponseDTO(
                u.getId(), u.getEmail(), u.getRole().name(), u.getNickname(), u.isEmailVerified()
        );
    }
}
