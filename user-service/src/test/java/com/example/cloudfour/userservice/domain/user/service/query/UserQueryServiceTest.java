package com.example.cloudfour.userservice.domain.user.service.query;

import com.example.cloudfour.userservice.domain.user.dto.AuthResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.enums.Role;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @org.mockito.InjectMocks
    UserQueryService sut;

    @org.mockito.Mock
    UserRepository userRepository;

    UUID uid;
    String emailLower;

    @BeforeEach
    void setUp() {
        uid = UUID.randomUUID();
        emailLower = "user@example.com";
    }

    @Test
    @DisplayName("byId: 미존재 시 UserException(USER_NOT_FOUND)")
    void byId_notFound_throws() {
        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.byId(uid))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("byId: 성공 시 Brief DTO 반환")
    void byId_success_returnsBrief() {
        var entity = mock(User.class);
        when(entity.getId()).thenReturn(uid);
        when(entity.getEmail()).thenReturn(emailLower);
        when(entity.getNickname()).thenReturn("닉네임");
        when(entity.getRole()).thenReturn(Role.ROLE_CUSTOMER);

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(entity));

        var out = sut.byId(uid);
        assertThat(out).isInstanceOf(AuthResponseDTO.UserBriefResponseDTO.class);
        var brief = (AuthResponseDTO.UserBriefResponseDTO) out;
        assertThat(brief.id()).isEqualTo(uid);
        assertThat(brief.email()).isEqualTo(emailLower);
        assertThat(brief.role()).isEqualTo(Role.ROLE_CUSTOMER.name());
        assertThat(brief.name()).isEqualTo("닉네임");
    }

    @Test
    @DisplayName("byEmail: 미존재 시 UserException(USER_NOT_FOUND)")
    void byEmail_notFound_throws() {
        when(userRepository.findByEmailAndIsDeletedFalse(emailLower)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.byEmail(emailLower))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.EMAIL_NOT_FOUND);
    }

    @Test
    @DisplayName("byEmail: 성공 시 Brief DTO 반환")
    void byEmail_success_returnsBrief() {
        var entity = mock(User.class);
        when(entity.getId()).thenReturn(uid);
        when(entity.getEmail()).thenReturn(emailLower);
        when(entity.getNickname()).thenReturn("사장님");
        when(entity.getRole()).thenReturn(Role.ROLE_OWNER);

        when(userRepository.findByEmailAndIsDeletedFalse(emailLower)).thenReturn(Optional.of(entity));

        var out = sut.byEmail(emailLower);
        assertThat(out).isInstanceOf(AuthResponseDTO.UserBriefResponseDTO.class);
        var brief = (AuthResponseDTO.UserBriefResponseDTO) out;
        assertThat(brief.id()).isEqualTo(uid);
        assertThat(brief.email()).isEqualTo(emailLower);
        assertThat(brief.role()).isEqualTo(Role.ROLE_OWNER.name());
        assertThat(brief.name()).isEqualTo("사장님");
    }

    @Test
    @DisplayName("existsByEmailBool: 존재 여부 boolean 반환")
    void existsByEmailBool() {
        when(userRepository.existsByEmailAndIsDeletedFalse(emailLower))
                .thenReturn(true, false);

        assertThat(sut.existsByEmail(emailLower)).isTrue();
        assertThat(sut.existsByEmail(emailLower)).isFalse();
    }
}
