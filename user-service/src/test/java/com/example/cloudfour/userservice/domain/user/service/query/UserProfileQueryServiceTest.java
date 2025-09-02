package com.example.cloudfour.userservice.domain.user.service;

import com.example.cloudfour.userservice.domain.user.converter.UserConverter;
import com.example.cloudfour.userservice.domain.user.dto.UserResponseDTO;
import com.example.cloudfour.userservice.domain.user.entity.User;
import com.example.cloudfour.userservice.domain.user.exception.UserErrorCode;
import com.example.cloudfour.userservice.domain.user.exception.UserException;
import com.example.cloudfour.userservice.domain.user.repository.UserRepository;
import com.example.cloudfour.userservice.domain.user.service.query.UserProfileQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileQueryServiceTest {

    @org.mockito.InjectMocks
    UserProfileQueryService sut;

    @org.mockito.Mock
    UserRepository userRepository;

    @Test
    @DisplayName("getMyInfo: 사용자 없음 -> USER_NOT_FOUND")
    void getMyInfo_userNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getMyInfo(userId))
                .isInstanceOf(UserException.class)
                .extracting("code")
                .isEqualTo(UserErrorCode.USER_NOT_FOUND);

        verify(userRepository).findByIdAndIsDeletedFalse(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    @DisplayName("getMyInfo: 성공 -> Converter로 매핑")
    void getMyInfo_success() {
        UUID userId = UUID.randomUUID();
        var entity = mock(User.class);
        when(userRepository.findByIdAndIsDeletedFalse(userId)).thenReturn(Optional.of(entity));

        try (MockedStatic<UserConverter> mocked = Mockito.mockStatic(UserConverter.class)) {
            var expected = UserResponseDTO.MeResponseDTO.builder()
                    .userId(userId)
                    .email("user@example.com")
                    .nickname("닉")
                    .build();
            mocked.when(() -> UserConverter.toMeResponseDTO(entity)).thenReturn(expected);

            var out = sut.getMyInfo(userId);
            assertThat(out).isSameAs(expected);
        }

        verify(userRepository).findByIdAndIsDeletedFalse(userId);
        verifyNoMoreInteractions(userRepository);
    }
}
