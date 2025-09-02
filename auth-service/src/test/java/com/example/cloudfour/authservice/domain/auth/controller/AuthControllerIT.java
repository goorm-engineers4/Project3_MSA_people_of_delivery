package com.example.cloudfour.authservice.domain.auth.controller;

import com.example.cloudfour.authservice.domain.auth.dto.AuthRequestDTO;
import com.example.cloudfour.authservice.domain.auth.dto.AuthResponseDTO;
import com.example.cloudfour.authservice.domain.auth.service.AuthService;
import com.example.cloudfour.modulecommon.dto.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.*;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.support.*;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.bind.support.WebDataBinderFactory;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController 통합 테스트")
class AuthControllerIT {

    @InjectMocks AuthController controller;
    @Mock AuthService authService;

    MockMvc mvc;
    ObjectMapper om;

    @BeforeEach
    void setUp() {
        om = new ObjectMapper();

        OncePerRequestFilter noopFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain
            ) throws java.io.IOException, jakarta.servlet.ServletException {
                filterChain.doFilter(request, response);
            }
        };

        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .apply(SecurityMockMvcConfigurers.springSecurity(noopFilter))
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver(),
                        currentUserFallback()
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private HandlerMethodArgumentResolver currentUserFallback() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().getName()
                        .equals("com.example.cloudfour.modulecommon.dto.CurrentUser");
            }
            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                return auth != null ? auth.getPrincipal() : null;
            }
        };
    }


    private RequestPostProcessor authPrincipal(UUID id) {
        return request -> {
            CurrentUser principal = Mockito.mock(CurrentUser.class);

            lenient().when(principal.id()).thenReturn(id);

            var auth = new UsernamePasswordAuthenticationToken(
                    principal, "N/A", java.util.Collections.emptyList());

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            request.setUserPrincipal(auth);
            return request;
        };
    }

    // -------- Register --------

    @Test
    @DisplayName("POST /auth/register/customer: 회원가입 성공")
    void register_customer_success() throws Exception {
        var req = new AuthRequestDTO.RegisterRequestDTO(
                "user@example.com", "닉네임", "Password!2", "ROLE_USER", "010-0000-0000");
        var resp = AuthResponseDTO.AuthRegisterResponseDTO.builder()
                .userId(UUID.randomUUID()).email("user@example.com").nickname("닉네임").role("ROLE_USER").build();

        when(authService.register(any(AuthRequestDTO.RegisterRequestDTO.class))).thenReturn(resp);

        mvc.perform(post("/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).register(any(AuthRequestDTO.RegisterRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/register/owner: 점주 회원가입 성공")
    void register_owner_success() throws Exception {
        var req = new AuthRequestDTO.RegisterRequestDTO(
                "owner@example.com", "사장", "Password!2", "ROLE_OWNER", "010-1111-2222");
        var resp = AuthResponseDTO.AuthRegisterResponseDTO.builder()
                .userId(UUID.randomUUID()).email("owner@example.com").nickname("사장").role("ROLE_OWNER").build();

        when(authService.register(any(AuthRequestDTO.RegisterRequestDTO.class))).thenReturn(resp);

        mvc.perform(post("/auth/register/owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).register(any(AuthRequestDTO.RegisterRequestDTO.class));
    }

    // -------- Login / Refresh / Logout --------

    @Test
    @DisplayName("POST /auth/login: 로그인 성공 시 토큰 반환")
    void login_success() throws Exception {
        var req = new AuthRequestDTO.LoginRequestDTO(
                "user@example.com",
                "Pw!12345"
        );
        var token = AuthResponseDTO.AuthTokenResponseDTO.builder()
                .accessToken("acc").refreshToken("ref").build();

        when(authService.login(any(AuthRequestDTO.LoginRequestDTO.class))).thenReturn(token);

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).login(any(AuthRequestDTO.LoginRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/login: 비밀번호 8자 미만이면 400")
    void login_password_too_short_400() throws Exception {
        var req = new AuthRequestDTO.LoginRequestDTO("user@example.com", "pw!"); // 3자

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /auth/refresh: 리프레시 성공 시 새 토큰 반환")
    void refresh_success() throws Exception {
        var req = new AuthRequestDTO.RefreshTokenRequestDTO("user@example.com", "ref1");
        var token = AuthResponseDTO.AuthTokenResponseDTO.builder()
                .accessToken("acc2").refreshToken("ref2").build();
        when(authService.refreshAccessToken(any(AuthRequestDTO.RefreshTokenRequestDTO.class))).thenReturn(token);

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).refreshAccessToken(any(AuthRequestDTO.RefreshTokenRequestDTO.class));
    }

    @Test
    @DisplayName("POST /auth/logout: Authorization 헤더 전달 시 로그아웃")
    void logout_success() throws Exception {
        mvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer abc.def.sig"))
                .andExpect(status().isOk());

        verify(authService).logout("Bearer abc.def.sig");
    }

    // -------- Password --------

    @Test
    @DisplayName("POST /auth/password: 인증된 사용자의 비밀번호 변경")
    void change_password_success() throws Exception {
        UUID uid = UUID.randomUUID();

        var req = new AuthRequestDTO.PasswordChangeDto(
                "CurrPass!1",
                "NewPass!2"
        );

        mvc.perform(post("/auth/password")
                        .with(authPrincipal(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).changePassword(eq(uid), any(AuthRequestDTO.PasswordChangeDto.class));
    }

    @Test
    @DisplayName("POST /auth/password: 새 비밀번호 8자 미만이면 400")
    void change_password_too_short_400() throws Exception {
        UUID uid = UUID.randomUUID();
        var req = new AuthRequestDTO.PasswordChangeDto("CurrPass!1", "new!");

        mvc.perform(post("/auth/password")
                        .with(authPrincipal(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // -------- Email Verify --------

    @Test
    @DisplayName("POST /auth/email/send: 이메일 인증 코드 발송")
    void send_email_code() throws Exception {
        var req = new AuthRequestDTO.EmailCodeRequestDTO("user@example.com");

        mvc.perform(post("/auth/email/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).sendVerificationEmail("user@example.com");
    }

    @Test
    @DisplayName("POST /auth/email/verify: 이메일 인증 코드 검증")
    void verify_email_code() throws Exception {
        var req = new AuthRequestDTO.EmailVerifyRequestDTO("user@example.com", "123456");

        mvc.perform(post("/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).verifyEmailCode(any(AuthRequestDTO.EmailVerifyRequestDTO.class));
    }

    // -------- Email Change --------

    @Test
    @DisplayName("POST /auth/email/change/start: 인증된 사용자의 이메일 변경 시작")
    void start_email_change() throws Exception {
        UUID uid = UUID.randomUUID();
        var req = new AuthRequestDTO.EmailChangeStartRequestDTO("new@example.com");

        mvc.perform(post("/auth/email/change/start")
                        .with(authPrincipal(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).startEmailChange(uid, "new@example.com");
    }

    @Test
    @DisplayName("POST /auth/email/change/verify: 인증된 사용자의 이메일 변경 검증")
    void verify_email_change() throws Exception {
        UUID uid = UUID.randomUUID();
        var req = new AuthRequestDTO.EmailChangeVerifyRequestDTO("new@example.com", "123456");

        mvc.perform(post("/auth/email/change/verify")
                        .with(authPrincipal(uid))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(authService).verifyEmailChange(uid, "new@example.com", "123456");
    }
}
