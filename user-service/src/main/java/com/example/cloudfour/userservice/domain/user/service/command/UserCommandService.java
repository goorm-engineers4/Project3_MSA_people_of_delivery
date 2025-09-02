package com.example.cloudfour.userservice.domain.user.service.command;

import com.example.cloudfour.userservice.domain.user.dto.AuthRequestDTO;
import com.example.cloudfour.userservice.domain.user.dto.AuthResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.enums.Role;
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
public class UserCommandService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponseDTO.UserBriefResponseDTO create(AuthRequestDTO.CreateUserRequestDTO req) {
        String email = req.email().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new UserException(UserErrorCode.EMAIL_ALREADY_USED);
        }
        Role role = Role.valueOf(req.role());

        User user = User.builder()
                .email(email)
                .nickname(req.nickname())
                .number(req.number())
                .password(passwordEncoder.encode(req.rawPassword()))
                .role(role)
                .build();

        userRepository.save(user);
        return new AuthResponseDTO.UserBriefResponseDTO(
                user.getId(), user.getEmail(), user.getRole().name(), user.getNickname(), user.isEmailVerified()
        );
    }

    @Transactional
    public void markEmailVerified(UUID id) {
        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        u.markEmailVerified();
    }

    @Transactional
    public void changePassword(UUID id, AuthRequestDTO.ChangePasswordRequestDTO req) {
        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        if (!passwordEncoder.matches(req.currentPassword(), u.getPassword())) {
            throw new IllegalStateException("PASSWORD_MISMATCH");
        }
        u.changePassword(passwordEncoder.encode(req.newPassword()));
    }

    @Transactional
    public void startEmailChange(UUID id, AuthRequestDTO.EmailChangeStartRequestDTO req) {
        String newEmail = req.newEmail().toLowerCase(Locale.ROOT);

        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (u.getEmail().equalsIgnoreCase(newEmail)) throw new UserException(UserErrorCode.EMAIL_SAME_AS_OLD);
        if (userRepository.existsByEmailAndIsDeletedFalse(newEmail)) throw new UserException(UserErrorCode.EMAIL_ALREADY_USED);

        u.requestEmailChange(newEmail);
    }

    @Transactional
    public void confirmEmailChange(UUID id, AuthRequestDTO.EmailChangeConfirmRequestDTO req) {
        String newEmail = req.newEmail().toLowerCase(Locale.ROOT);

        var u = userRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        if (u.getPendingEmail() == null || !u.getPendingEmail().equalsIgnoreCase(newEmail)) {
            throw new UserException(UserErrorCode.PENDING_EMAIL_MISMATCH);
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(newEmail)) {
            throw new UserException(UserErrorCode.EMAIL_ALREADY_USED);
        }

        u.confirmEmailChange();
    }
}
