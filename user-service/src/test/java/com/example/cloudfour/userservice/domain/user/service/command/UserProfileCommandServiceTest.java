package com.example.cloudfour.userservice.domain.user.service.command;

import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileCommandServiceTest {

    @org.mockito.InjectMocks
    UserProfileCommandService sut;

    @org.mockito.Mock
    UserRepository userRepository;

    UUID uid;

    @BeforeEach
    void init() {
        uid = UUID.randomUUID();
    }

    @Test
    @DisplayName("updateProfile: 사용자 없음 -> USER_NOT_FOUND")
    void updateProfile_userNotFound() {
        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateProfile(uid, "새닉", "010-0000-0000"))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("updateProfile: 닉네임/번호 변경 성공 (save 호출 없음)")
    void updateProfile_success_changesFields() {
        var user = User.builder()
                .email("old@example.com")
                .nickname("old")
                .number("010-1111-2222")
                .build();

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(user));

        sut.updateProfile(uid, "newNick", "010-0000-0000");

        assertThat(user.getNickname()).isEqualTo("newNick");
        assertThat(user.getNumber()).isEqualTo("010-0000-0000");

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("deleteAccount: soft-delete 수행 (save 호출 없음)")
    void deleteAccount_success() {
        var user = spy(User.builder()
                .email("user@example.com")
                .nickname("닉")
                .number("010-0000-0000")
                .build());

        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.of(user));

        sut.deleteAccount(uid);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("deleteAccount: 사용자 없음 -> USER_NOT_FOUND")
    void deleteAccount_userNotFound() {
        when(userRepository.findByIdAndIsDeletedFalse(uid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.deleteAccount(uid))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByIdAndIsDeletedFalse(uid);
        verifyNoMoreInteractions(userRepository);
    }
}
