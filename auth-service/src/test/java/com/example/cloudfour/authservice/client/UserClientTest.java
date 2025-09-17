package com.example.cloudfour.authservice.client;

import com.example.cloudfour.authservice.domain.auth.dto.UserRequestDTO;
import com.example.cloudfour.authservice.domain.auth.dto.UserResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserClient 단위 테스트")
class UserClientTest {

    @Mock UserFeignClient feign;
    @InjectMocks UserClient client;

    @Test
    @DisplayName("existsByEmailBool: 응답 true/false/null 처리")
    void existsByEmailBool_cases() {
        when(feign.existsByEmail("a@x.com")).thenReturn(new UserResponseDTO.ExistsByEmailResponseDTO(true));
        when(feign.existsByEmail("b@x.com")).thenReturn(new UserResponseDTO.ExistsByEmailResponseDTO(false));
        when(feign.existsByEmail("c@x.com")).thenReturn(null);

        assertThat(client.existsByEmailBool("a@x.com")).isTrue();
        assertThat(client.existsByEmailBool("b@x.com")).isFalse();
        assertThat(client.existsByEmailBool("c@x.com")).isFalse();
    }

    @Test
    @DisplayName("changePassword: DTO 구성 및 위임")
    void changePassword_delegates_with_dto() {
        UUID id = UUID.randomUUID();
        ArgumentCaptor<UserRequestDTO.ChangePasswordRequestDTO> cap = ArgumentCaptor.forClass(UserRequestDTO.ChangePasswordRequestDTO.class);

        client.changePassword(id, "curr", "next");

        verify(feign).changePassword(eq(id), cap.capture());
        assertThat(cap.getValue().currentPassword()).isEqualTo("curr");
        assertThat(cap.getValue().newPassword()).isEqualTo("next");
    }

    @Test
    @DisplayName("verifyPassword: DTO 구성 및 위임")
    void verifyPassword_delegates_with_dto() {
        UUID id = UUID.randomUUID();
        when(feign.verifyPassword(eq(id), any(UserRequestDTO.PasswordVerifyRequestDTO.class)))
                .thenReturn(new UserResponseDTO.PasswordVerifyResponseDTO(true));

        var res = client.verifyPassword(id, "pw!");
        assertThat(res).isNotNull();

        ArgumentCaptor<UserRequestDTO.PasswordVerifyRequestDTO> cap = ArgumentCaptor.forClass(UserRequestDTO.PasswordVerifyRequestDTO.class);
        verify(feign).verifyPassword(eq(id), cap.capture());
        assertThat(cap.getValue().rawPassword()).isEqualTo("pw!");
    }

    @Test
    @DisplayName("start/confirmEmailChange: DTO 구성 및 위임")
    void email_change_delegation() {
        UUID id = UUID.randomUUID();

        client.startEmailChange(id, "new@x.com");
        ArgumentCaptor<UserRequestDTO.EmailChangeStartRequestDTO> capStart = ArgumentCaptor.forClass(UserRequestDTO.EmailChangeStartRequestDTO.class);
        verify(feign).startEmailChange(eq(id), capStart.capture());
        assertThat(capStart.getValue().newEmail()).isEqualTo("new@x.com");

        client.confirmEmailChange(id, "new@x.com");
        ArgumentCaptor<UserRequestDTO.EmailChangeConfirmRequestDTO> capConf = ArgumentCaptor.forClass(UserRequestDTO.EmailChangeConfirmRequestDTO.class);
        verify(feign).confirmEmailChange(eq(id), capConf.capture());
        assertThat(capConf.getValue().newEmail()).isEqualTo("new@x.com");
    }
}
