package com.example.cloudfour.authservice.domain.auth.service;

import com.example.cloudfour.authservice.client.UserClient;
import com.example.cloudfour.authservice.domain.auth.converter.AuthConverter;
import com.example.cloudfour.authservice.domain.auth.dto.*;
import com.example.cloudfour.authservice.domain.auth.enums.VerificationPurpose;
import com.example.cloudfour.authservice.domain.auth.exception.AuthErrorCode;
import com.example.cloudfour.authservice.domain.auth.exception.AuthException;
import com.example.cloudfour.authservice.util.RedisUtil;
import com.example.cloudfour.authservice.util.VerificationCodeHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @InjectMocks AuthService sut;

    @Mock UserClient userClient;
    @Mock EmailService emailService;
    @Mock RedisUtil redisUtil;
    @Mock JwtService jwtService;
    @Mock VerificationCodeHasher hasher;

    UUID uid;
    String emailLower;
    String emailUpper;

    @BeforeEach
    void setUp() {
        uid = UUID.randomUUID();
        emailLower = "user@example.com";
        emailUpper = "USER@EXAMPLE.COM";
    }

    private static String codeKey(String purpose, String emailLowerCase) {
        return "email:verify:code:" + purpose + ":" + emailLowerCase;
    }
    private static String triesKey(String purpose, String emailLowerCase) {
        return "email:verify:tries:" + purpose + ":" + emailLowerCase;
    }
    private static String resendKey(String purpose, String emailLowerCase) {
        return "email:verify:resend:" + purpose + ":" + emailLowerCase;
    }

    /** 공통 예외 검증 헬퍼: AuthException.code == expected */
    private void assertAuthThrows(Runnable call, AuthErrorCode expected) {
        assertThatThrownBy(() -> call.run())
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", expected);
    }

    // -------- Register --------
    @Nested
    @DisplayName("회원가입(register)")
    class Register {

        @Test
        @DisplayName("이미 존재하는 이메일이면 AuthException(EMAIL_ALREADY_USED)")
        void register_existingEmail_throws() {
            var req = new AuthRequestDTO.RegisterRequestDTO(
                    emailUpper, "닉", "Pw!234567", "USER", "010-0000-0000"
            );
            when(userClient.existsByEmailBool(emailLower)).thenReturn(true);

            assertAuthThrows(() -> sut.register(req), AuthErrorCode.EMAIL_ALREADY_USED);
        }

        @Test
        @DisplayName("정상 회원가입 시 UserClient.create 호출 후 Converter로 응답 매핑")
        void register_success() {
            var req = new AuthRequestDTO.RegisterRequestDTO(
                    emailUpper, "닉", "Pw!234567", "USER", "010-0000-0000"
            );

            when(userClient.existsByEmailBool(emailLower)).thenReturn(false);

            // userClient.create(...) 가 반환하는 요약 사용자 (record)
            var created = new UserResponseDTO.UserBriefResponseDTO(
                    uid, emailLower, "USER", "이름", true
            );
            when(userClient.create(any(UserRequestDTO.CreateUserRequestDTO.class)))
                    .thenReturn(created);

            // Converter static mocking
            try (MockedStatic<AuthConverter> mocked = mockStatic(AuthConverter.class)) {
                var model = new AuthModelDTO.RegisterResultDTO(
                        created.id(), created.email(), req.nickname(), created.role()
                );
                var expected = AuthResponseDTO.AuthRegisterResponseDTO.builder()
                        .userId(uid).email(emailLower).nickname("닉").role("USER").build();

                mocked.when(() -> AuthConverter.toAuthRegisterResponseDTO(model))
                        .thenReturn(expected);

                var out = sut.register(req);
                assertThat(out).isSameAs(expected);
                verify(userClient).create(any(UserRequestDTO.CreateUserRequestDTO.class));
            }
        }
    }

    // -------- Login --------
    @Nested
    @DisplayName("로그인(login)")
    class Login {

        @Test
        @DisplayName("이메일 미인증이면 AuthException(EMAIL_NOT_VERIFIED)")
        void login_unverified_throws() {
            var req = new AuthRequestDTO.LoginRequestDTO(emailUpper, "pw!");
            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", false);
            when(userClient.byEmail(emailLower)).thenReturn(user);

            assertAuthThrows(() -> sut.login(req), AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        @Test
        @DisplayName("비밀번호 불일치 시 AuthException(PASSWORD_INVALID)")
        void login_password_invalid_throws() {
            var req = new AuthRequestDTO.LoginRequestDTO(emailLower, "pw!");
            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byEmail(emailLower)).thenReturn(user);
            when(userClient.verifyPassword(uid, "pw!"))
                    .thenReturn(new UserResponseDTO.PasswordVerifyResponseDTO(false));

            assertAuthThrows(() -> sut.login(req), AuthErrorCode.PASSWORD_INVALID);
        }

        @Test
        @DisplayName("성공 시 Access/Refresh 발급 및 Refresh Redis 저장")
        void login_success_issuesTokensAndSaveRefresh() {
            var req = new AuthRequestDTO.LoginRequestDTO(emailLower, "pw!");
            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byEmail(emailLower)).thenReturn(user);
            when(userClient.verifyPassword(uid, "pw!"))
                    .thenReturn(new UserResponseDTO.PasswordVerifyResponseDTO(true));

            when(jwtService.createAccess(uid, "USER")).thenReturn("access");
            when(jwtService.createRefresh(uid, "USER")).thenReturn("refresh");
            when(jwtService.accessTtlSeconds()).thenReturn(3600L);

            try (MockedStatic<AuthConverter> mocked = mockStatic(AuthConverter.class)) {
                var resp = AuthResponseDTO.AuthTokenResponseDTO.builder()
                        .accessToken("access").refreshToken("refresh").build();

                mocked.when(() -> AuthConverter.toAuthTokenResponseDTO(Mockito.any(TokenDTO.class)))
                        .thenReturn(resp);

                var out = sut.login(req);

                assertThat(out).isSameAs(resp);
                verify(redisUtil).save(emailLower, "refresh");
            }
        }
    }

    // -------- Logout --------
    @Test
    @DisplayName("로그아웃 시 사용자 이메일 키의 Refresh 삭제")
    void logout_deletes_refresh() {
        var encoded = "header.body.sig";
        when(jwtService.userId(encoded)).thenReturn(uid.toString());

        var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
        when(userClient.byId(uid)).thenReturn(user);

        sut.logout("Bearer " + encoded);

        verify(redisUtil).delete(emailLower);
    }

    // -------- Refresh Access --------
    @Nested
    @DisplayName("Access 재발급(refreshAccessToken)")
    class RefreshAccess {

        @Test
        @DisplayName("유효하지 않은 토큰이면 AuthException(TOKEN_INVALID)")
        void invalid_token() {
            var req = new AuthRequestDTO.RefreshTokenRequestDTO(emailLower, "bad");
            when(jwtService.isValid("bad")).thenReturn(false);

            assertAuthThrows(() -> sut.refreshAccessToken(req), AuthErrorCode.TOKEN_INVALID);
        }

        @Test
        @DisplayName("typ != refresh 이면 AuthException(TOKEN_TYPE_INVALID)")
        void wrong_type_token() {
            var req = new AuthRequestDTO.RefreshTokenRequestDTO(emailLower, "ok");
            when(jwtService.isValid("ok")).thenReturn(true);

            var jwt = Jwt.withTokenValue("ok")
                    .header("alg", "none")
                    .claim("typ", "access")
                    .subject(uid.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(jwtService.decode("ok")).thenReturn(jwt);

            assertAuthThrows(() -> sut.refreshAccessToken(req), AuthErrorCode.TOKEN_TYPE_INVALID);
        }

        @Test
        @DisplayName("Redis 저장 Refresh와 불일치 시 AuthException(REFRESH_NOT_MATCHED)")
        void refresh_not_matched() {
            var req = new AuthRequestDTO.RefreshTokenRequestDTO(emailLower, "ref1");
            when(jwtService.isValid("ref1")).thenReturn(true);

            var jwt = Jwt.withTokenValue("ref1")
                    .header("alg", "none")
                    .claim("typ", "refresh")
                    .subject(uid.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(jwtService.decode("ref1")).thenReturn(jwt);

            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byId(uid)).thenReturn(user);
            when(redisUtil.get(emailLower)).thenReturn("refX");

            assertAuthThrows(() -> sut.refreshAccessToken(req), AuthErrorCode.REFRESH_NOT_MATCHED);
        }

        @Test
        @DisplayName("성공 시 Access 재발급 + Refresh 회전 저장")
        void refresh_success_rotate() {
            var req = new AuthRequestDTO.RefreshTokenRequestDTO(emailLower, "ref1");
            when(jwtService.isValid("ref1")).thenReturn(true);

            var jwt = Jwt.withTokenValue("ref1")
                    .header("alg", "none")
                    .claim("typ", "refresh")
                    .subject(uid.toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            when(jwtService.decode("ref1")).thenReturn(jwt);

            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byId(uid)).thenReturn(user);

            when(redisUtil.get(emailLower)).thenReturn("ref1");
            when(jwtService.createAccess(uid, "USER")).thenReturn("acc2");
            when(jwtService.createRefresh(uid, "USER")).thenReturn("ref2");
            when(jwtService.accessTtlSeconds()).thenReturn(1800L);

            try (MockedStatic<AuthConverter> mocked = mockStatic(AuthConverter.class)) {
                var resp = AuthResponseDTO.AuthTokenResponseDTO.builder()
                        .accessToken("acc2").refreshToken("ref2").build();

                mocked.when(() -> AuthConverter.toAuthTokenResponseDTO(Mockito.any(TokenDTO.class)))
                        .thenReturn(resp);

                var out = sut.refreshAccessToken(req);
                assertThat(out).isSameAs(resp);
                verify(redisUtil).save(emailLower, "ref2");
            }
        }
    }

    // -------- Change Password --------
    @Test
    @DisplayName("비밀번호 변경 시 UserClient 변경 후 기존 Refresh 삭제")
    void changePassword_deletes_refresh() {
        var req = new AuthRequestDTO.PasswordChangeDto("curr", "new");
        var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
        when(userClient.byId(uid)).thenReturn(user);

        sut.changePassword(uid, req);

        verify(userClient).changePassword(uid, "curr", "new");
        verify(redisUtil).delete(emailLower);
    }

    // -------- Email Verify --------
    @Nested
    @DisplayName("이메일 인증 코드 발송(sendVerificationEmail)")
    class SendVerificationEmail {

        @Test
        @DisplayName("쿨다운 중이면 AuthException(EMAIL_RESEND_COOLDOWN)")
        void cooldown_throws() {
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60)))
                    .thenReturn(false);

            assertAuthThrows(() -> sut.sendVerificationEmail(emailUpper), AuthErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        @Test
        @DisplayName("신규 코드 저장 시 시도횟수 키 삭제 후 메일 발송")
        void new_code_stores_and_sends() throws Exception {
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60)))
                    .thenReturn(true);
            when(hasher.hash(anyString())).thenReturn("hashed");
            when(redisUtil.setIfAbsent(codeKey(purpose, emailLower), "hashed", Duration.ofMinutes(10)))
                    .thenReturn(true);

            sut.sendVerificationEmail(emailUpper);

            verify(redisUtil).delete(triesKey(purpose, emailLower));
            verify(emailService).sendSimpleMessage(eq(emailLower), anyString(), contains("인증"));
        }

        @Test
        @DisplayName("기존 코드 존재 시 TTL 연장 후 메일 발송")
        void existing_code_extend_and_sends() throws Exception {
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60)))
                    .thenReturn(true);
            when(hasher.hash(anyString())).thenReturn("hashed");
            when(redisUtil.setIfAbsent(codeKey(purpose, emailLower), "hashed", Duration.ofMinutes(10)))
                    .thenReturn(false);

            sut.sendVerificationEmail(emailUpper);

            verify(redisUtil).expire(codeKey(purpose, emailLower), Duration.ofMinutes(10));
            verify(emailService).sendSimpleMessage(eq(emailLower), anyString(), anyString());
        }

        @Test
        @DisplayName("메일 전송 실패 시 코드/쿨다운 키 정리 후 AuthException(EMAIL_SEND_FAILED)")
        void send_mail_fail_cleanup_and_throw() throws Exception {
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60)))
                    .thenReturn(true);
            when(hasher.hash(anyString())).thenReturn("hashed");
            when(redisUtil.setIfAbsent(codeKey(purpose, emailLower), "hashed", Duration.ofMinutes(10)))
                    .thenReturn(true);

            doThrow(new RuntimeException("mail fail"))
                    .when(emailService).sendSimpleMessage(anyString(), anyString(), anyString());

            assertAuthThrows(() -> sut.sendVerificationEmail(emailUpper), AuthErrorCode.EMAIL_SEND_FAILED);

            verify(redisUtil).delete(codeKey(purpose, emailLower));
            verify(redisUtil).delete(resendKey(purpose, emailLower));
        }
    }

    @Nested
    @DisplayName("이메일 인증 코드 검증(verifyEmailCode)")
    class VerifyEmailCode {

        @Test
        @DisplayName("코드 만료 시 AuthException(EMAIL_CODE_EXPIRED)")
        void expired() {
            var req = new AuthRequestDTO.EmailVerifyRequestDTO(emailUpper, "123456");
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();

            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn(null);

            assertAuthThrows(() -> sut.verifyEmailCode(req), AuthErrorCode.EMAIL_CODE_EXPIRED);
        }

        @Test
        @DisplayName("시도횟수 초과 시 AuthException(EMAIL_CODE_TRY_EXCEEDED)")
        void try_exceeded() {
            var req = new AuthRequestDTO.EmailVerifyRequestDTO(emailUpper, "123456");
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();

            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn("hashed");
            when(redisUtil.incrWithTtl(triesKey(purpose, emailLower), Duration.ofMinutes(10)))
                    .thenReturn(6L);

            assertAuthThrows(() -> sut.verifyEmailCode(req), AuthErrorCode.EMAIL_CODE_TRY_EXCEEDED);
        }

        @Test
        @DisplayName("코드 불일치 시 AuthException(EMAIL_CODE_INVALID)")
        void code_mismatch() {
            var req = new AuthRequestDTO.EmailVerifyRequestDTO(emailUpper, "000000");
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();

            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn("hashed-stored");
            when(redisUtil.incrWithTtl(triesKey(purpose, emailLower), Duration.ofMinutes(10))).thenReturn(1L);
            when(hasher.hash("000000")).thenReturn("hashed-input");

            assertAuthThrows(() -> sut.verifyEmailCode(req), AuthErrorCode.EMAIL_CODE_INVALID);
        }

        @Test
        @DisplayName("성공 시 사용자 인증 완료 & 키 삭제")
        void success() {
            var req = new AuthRequestDTO.EmailVerifyRequestDTO(emailUpper, "123456");
            var purpose = VerificationPurpose.EMAIL_VERIFY.name();

            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn("H");
            when(redisUtil.incrWithTtl(triesKey(purpose, emailLower), Duration.ofMinutes(10))).thenReturn(1L);
            when(hasher.hash("123456")).thenReturn("H");

            var user = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byEmail(emailLower)).thenReturn(user);

            sut.verifyEmailCode(req);

            verify(userClient).markEmailVerified(uid);
            verify(redisUtil).delete(codeKey(purpose, emailLower));
            verify(redisUtil).delete(triesKey(purpose, emailLower));
            verify(redisUtil).delete(resendKey(purpose, emailLower));
        }
    }

    // -------- Change Email --------
    @Nested
    @DisplayName("이메일 변경 시작(startEmailChange)")
    class StartEmailChange {

        @Test
        @DisplayName("기존 이메일과 동일하면 AuthException(EMAIL_SAME_AS_OLD)")
        void same_as_old() {
            var current = new UserResponseDTO.UserBriefResponseDTO(uid, emailLower, "USER", "이름", true);
            when(userClient.byId(uid)).thenReturn(current);

            assertAuthThrows(() -> sut.startEmailChange(uid, emailUpper), AuthErrorCode.EMAIL_SAME_AS_OLD);
        }

        @Test
        @DisplayName("이미 사용 중인 이메일이면 AuthException(EMAIL_IN_USE)")
        void email_in_use() {
            when(userClient.byId(uid)).thenReturn(new UserResponseDTO.UserBriefResponseDTO(uid, "old@x.com", "USER", "이름", true));
            when(userClient.existsByEmailBool(emailLower)).thenReturn(true);

            assertAuthThrows(() -> sut.startEmailChange(uid, emailUpper), AuthErrorCode.EMAIL_IN_USE);
        }

        @Test
        @DisplayName("쿨다운 중이면 AuthException(EMAIL_RESEND_COOLDOWN)")
        void cooldown() {
            when(userClient.byId(uid)).thenReturn(new UserResponseDTO.UserBriefResponseDTO(uid, "old@x.com", "USER", "이름", true));
            when(userClient.existsByEmailBool(emailLower)).thenReturn(false);

            var purpose = VerificationPurpose.CHANGE_EMAIL.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60))).thenReturn(false);

            assertAuthThrows(() -> sut.startEmailChange(uid, emailUpper), AuthErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        @Test
        @DisplayName("성공 시 코드 저장 & 메일 발송 & 시도키 초기화")
        void success() throws Exception {
            when(userClient.byId(uid)).thenReturn(new UserResponseDTO.UserBriefResponseDTO(uid, "old@x.com", "USER", "이름", true));
            when(userClient.existsByEmailBool(emailLower)).thenReturn(false);

            var purpose = VerificationPurpose.CHANGE_EMAIL.name();
            when(redisUtil.setIfAbsent(resendKey(purpose, emailLower), "1", Duration.ofSeconds(60))).thenReturn(true);
            when(hasher.hash(anyString())).thenReturn("hashed");

            sut.startEmailChange(uid, emailUpper);

            verify(userClient).startEmailChange(uid, emailLower);
            verify(redisUtil).setWithTtl(codeKey(purpose, emailLower), "hashed", Duration.ofMinutes(10));
            verify(redisUtil).delete(triesKey(purpose, emailLower));
            verify(emailService).sendSimpleMessage(eq(emailLower), anyString(), contains("인증"));
        }
    }

    @Nested
    @DisplayName("이메일 변경 검증(verifyEmailChange)")
    class VerifyEmailChange {

        @Test
        @DisplayName("코드 만료 시 AuthException(EMAIL_CODE_EXPIRED)")
        void expired_or_invalid() {
            var purpose = VerificationPurpose.CHANGE_EMAIL.name();

            when(userClient.existsByEmailBool(emailLower)).thenReturn(false);
            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn(null);

            assertAuthThrows(() -> sut.verifyEmailChange(uid, emailUpper, "000000"),
                    AuthErrorCode.EMAIL_CODE_EXPIRED);
        }

        @Test
        @DisplayName("성공 시 이메일 변경 확정 & 키 삭제")
        void success() {
            var purpose = VerificationPurpose.CHANGE_EMAIL.name();

            when(userClient.existsByEmailBool(emailLower)).thenReturn(false);
            when(redisUtil.get(codeKey(purpose, emailLower))).thenReturn("H");
            when(redisUtil.incrWithTtl(triesKey(purpose, emailLower), Duration.ofMinutes(10))).thenReturn(1L);
            when(hasher.hash("123456")).thenReturn("H");

            sut.verifyEmailChange(uid, emailUpper, "123456");

            verify(userClient).confirmEmailChange(uid, emailLower);
            verify(redisUtil).delete(codeKey(purpose, emailLower));
            verify(redisUtil).delete(triesKey(purpose, emailLower));
            verify(redisUtil).delete(resendKey(purpose, emailLower));
        }
    }
}
