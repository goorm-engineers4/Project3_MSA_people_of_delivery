package com.example.cloudfour.userservice.domain.user.service.command;

import com.example.cloudfour.userservice.domain.user.dto.AuthRequestDTO;
import com.example.cloudfour.userservice.domain.user.dto.AuthResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.enums.Role;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceTest {

    @InjectMocks UserCommandService sut;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;

    UUID uid;
    String emailLower, emailUpper;

    @BeforeEach
    void setUp() {
        uid = UUID.randomUUID();
        emailLower = "user@example.com";
        emailUpper = "USER@EXAMPLE.COM";
    }

    @Test
    @DisplayName("회원 생성: 중복 이메일이면 UserException(EMAIL_ALREADY_USED)")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmailAndIsDeletedFalse(emailLower)).thenReturn(true);

        var req = new AuthRequestDTO.CreateUserRequestDTO(
                // (email, nickname, phone, password, role)
                emailUpper, "닉", "010-0000-0000", "Pw!234567", "ROLE_CUSTOMER"
        );

        assertThatThrownBy(() -> sut.create(req))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.EMAIL_ALREADY_USED);
    }

    @Test
    @DisplayName("회원 생성: 성공 시 저장 & Brief 반환")
    void register_success() {

        when(userRepository.existsByEmailAndIsDeletedFalse(emailLower)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0, User.class));
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "ENC(" + inv.getArgument(0) + ")");

        var req = new AuthRequestDTO.CreateUserRequestDTO(
                emailUpper, "닉네임", "010-0000-0000", "Pw!234567", "ROLE_CUSTOMER"
        );

        Object out = sut.create(req);

        assertThat(out).isInstanceOf(AuthResponseDTO.UserBriefResponseDTO.class);
        var brief = (AuthResponseDTO.UserBriefResponseDTO) out;
        assertThat(brief.id()).isNull();
        assertThat(brief.email()).isEqualTo(emailLower);
        assertThat(brief.role()).isEqualTo("ROLE_CUSTOMER");
        assertThat(brief.name()).isEqualTo("닉네임");
        assertThat(brief.emailVerified()).isFalse();

        verify(userRepository).existsByEmailAndIsDeletedFalse(emailLower);
        verify(passwordEncoder).encode("Pw!234567");
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("비밀번호 변경: 사용자 조회 실패 시 UserException(USER_NOT_FOUND)")
    void changePassword_userNotFound() {
        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.empty());

        var dto = new AuthRequestDTO.ChangePasswordRequestDTO("curr", "new!Pass123");

        assertThatThrownBy(() -> sut.changePassword(uid, dto))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("비밀번호 변경: 현재 비번 일치하면 인코딩 후 엔티티만 수정(저장 호출 없음)")
    void changePassword_success() {
        var user = User.builder()
                .email(emailLower)
                .nickname("닉")
                .role(Role.ROLE_CUSTOMER)
                .emailVerified(true)
                .build();

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(user));

        var dto = new AuthRequestDTO.ChangePasswordRequestDTO("curr!1234", "New!123456");

        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        when(passwordEncoder.encode("New!123456")).thenReturn("ENC(New!123456)");

        sut.changePassword(uid, dto);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verify(passwordEncoder).matches(eq("curr!1234"), any());
        verify(passwordEncoder).encode("New!123456");

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("이메일 변경 시작: 사용자 없으면 UserException(USER_NOT_FOUND)")
    void startEmailChange_userNotFound() {
        var req = new AuthRequestDTO.EmailChangeStartRequestDTO(emailUpper);

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.startEmailChange(uid, req))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("이메일 변경 시작: 신규 이메일이 이미 사용중이면 UserException(EMAIL_ALREADY_USED)")
    void startEmailChange_inUse() {
        var user = User.builder()
                .email("old@x.com")
                .nickname("닉")
                .role(Role.ROLE_CUSTOMER)
                .emailVerified(true)
                .build();

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIsDeletedFalse(emailLower)).thenReturn(true);

        var req = new AuthRequestDTO.EmailChangeStartRequestDTO(emailUpper);

        assertThatThrownBy(() -> sut.startEmailChange(uid, req))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.EMAIL_ALREADY_USED);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verify(userRepository).existsByEmailAndIsDeletedFalse(emailLower);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("이메일 변경 확정: pendingEmail 일치 시 이메일만 업데이트(저장 호출 없음)")
    void confirmEmailChange_success() {
        var user = User.builder()
                .email("old@x.com")
                .nickname("닉")
                .role(Role.ROLE_CUSTOMER)
                .emailVerified(true)
                .build();

        user.requestEmailChange(emailLower);

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailAndIsDeletedFalse(emailLower)).thenReturn(false);

        var req = new AuthRequestDTO.EmailChangeConfirmRequestDTO(emailUpper);

        sut.confirmEmailChange(uid, req);

        assertThat(user.getEmail()).isEqualTo(emailLower);
        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verify(userRepository).existsByEmailAndIsDeletedFalse(emailLower);

        verifyNoMoreInteractions(userRepository);
    }
}
