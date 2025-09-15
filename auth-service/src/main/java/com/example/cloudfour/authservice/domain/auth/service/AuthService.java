package com.example.cloudfour.authservice.domain.auth.service;

import com.example.cloudfour.authservice.client.UserClient;
import com.example.cloudfour.authservice.domain.auth.dto.AuthModelDTO;
import com.example.cloudfour.authservice.domain.auth.dto.UserRequestDTO;
import com.example.cloudfour.authservice.domain.auth.converter.AuthConverter;
import com.example.cloudfour.authservice.domain.auth.dto.AuthRequestDTO;
import com.example.cloudfour.authservice.domain.auth.dto.AuthResponseDTO;
import com.example.cloudfour.authservice.domain.auth.dto.TokenDTO;
import com.example.cloudfour.authservice.domain.auth.enums.VerificationPurpose;
import com.example.cloudfour.authservice.domain.auth.exception.AuthErrorCode;
import com.example.cloudfour.authservice.domain.auth.exception.AuthException;
import com.example.cloudfour.authservice.util.RedisUtil;
import com.example.cloudfour.authservice.util.VerificationCodeHasher;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserClient userClient;
    private final EmailService emailService;
    private final RedisUtil redisUtil;
    private final JwtService jwtService;
    private final VerificationCodeHasher hasher;

    private static final int CODE_LEN = 6;
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_TTL = Duration.ofSeconds(60);
    private static final int MAX_TRIES = 5;

    private String codeKey(String purpose, String email)   { return "email:verify:code:"   + purpose + ":" + email; }
    private String triesKey(String purpose, String email)  { return "email:verify:tries:"  + purpose + ":" + email; }
    private String resendKey(String purpose, String email) { return "email:verify:resend:" + purpose + ":" + email; }

    public AuthResponseDTO.AuthRegisterResponseDTO register(AuthRequestDTO.RegisterRequestDTO request){
        String email = request.email().toLowerCase();

        if (userClient.existsByEmailBool(email)) {
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_USED);
        }

        var created = userClient.create(new UserRequestDTO.CreateUserRequestDTO(
                email,
                request.nickname(),
                request.number(),
                request.password(),
                request.role()
        ));

        var model = new AuthModelDTO.RegisterResultDTO(
                created.id(),
                created.email(),
                request.nickname(),
                created.role()
        );

        return AuthConverter.toAuthRegisterResponseDTO(model);
    }

    public AuthResponseDTO.AuthTokenResponseDTO login(AuthRequestDTO.LoginRequestDTO request) {
        String email = request.email().toLowerCase();

        var user = userClient.byEmail(email);

        if (!user.emailVerified()) {
            throw new AuthException(AuthErrorCode.EMAIL_NOT_VERIFIED);
        }

        var pw = userClient.verifyPassword(user.id(), request.password());
        if (pw == null || !pw.match()) {
            throw new AuthException(AuthErrorCode.PASSWORD_INVALID);
        }

        log.info("로그인 성공: userId={}", user.id());

        String access  = jwtService.createAccess(user.id(), user.role());
        String refresh = jwtService.createRefresh(user.id(), user.role());

        var token = new TokenDTO("Bearer", access, refresh, jwtService.accessTtlSeconds());
        redisUtil.save(user.email(), refresh);

        return AuthConverter.toAuthTokenResponseDTO(token);
    }

    public void logout(String accessHeader) {
        String token = stripBearer(accessHeader);
        String userId = jwtService.userId(token);
        var user = userClient.byId(UUID.fromString(userId));

        redisUtil.delete(user.email());
        log.info("로그아웃: userId={}", user.id());
    }

    public AuthResponseDTO.AuthTokenResponseDTO refreshAccessToken(AuthRequestDTO.RefreshTokenRequestDTO request) {
        String refresh = request.refreshToken();

        if (refresh == null || !jwtService.isValid(refresh)) {
            throw new AuthException(AuthErrorCode.TOKEN_INVALID);
        }

        var jwt = jwtService.decode(refresh);
        String typ = jwt.getClaimAsString("typ");
        if (!"refresh".equals(typ)) {
            throw new AuthException(AuthErrorCode.TOKEN_TYPE_INVALID);
        }

        String userId = jwt.getSubject();
        var user = userClient.byId(UUID.fromString(userId));

        String savedRefreshToken = redisUtil.get(user.email());
        if (savedRefreshToken == null || !savedRefreshToken.equals(refresh)) {
            throw new AuthException(AuthErrorCode.REFRESH_NOT_MATCHED);
        }

        String access  = jwtService.createAccess(user.id(), user.role());
        String newRefresh = jwtService.createRefresh(user.id(), user.role());
        redisUtil.save(user.email(), newRefresh);

        var token = new TokenDTO("Bearer", access, newRefresh, jwtService.accessTtlSeconds());
        return AuthConverter.toAuthTokenResponseDTO(token);
    }

    public void changePassword(UUID userId, AuthRequestDTO.PasswordChangeDto request) {
        userClient.changePassword(userId, request.currentPassword(), request.newPassword());

        var user = userClient.byId(userId);
        redisUtil.delete(user.email());
    }

    public void sendVerificationEmail(String email) {
        Objects.requireNonNull(email, "email은 null일 수 없습니다.");
        String target = email.toLowerCase();
        String purpose = VerificationPurpose.EMAIL_VERIFY.name();

        if (!redisUtil.setIfAbsent(resendKey(purpose, target), "1", RESEND_TTL)) {
            throw new AuthException(AuthErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        String code = generateCode(CODE_LEN);
        String hashed = hasher.hash(code);

        if (!redisUtil.setIfAbsent(codeKey(purpose, target), hashed, CODE_TTL)) {
            redisUtil.expire(codeKey(purpose, target), CODE_TTL);
        } else {
            redisUtil.delete(triesKey(purpose, target));
        }

        String title = "이메일 인증 번호";
        String content = """
                <html><body>
                <h1>인증 코드 : %s</h1>
                <p>해당 코드를 홈페이지에 입력하세요.</p>
                <p>* 본 메일은 자동응답 메일입니다.</p>
                </body></html>
                """.formatted(code);
        try {
            emailService.sendSimpleMessage(target, title, content);
        } catch (RuntimeException | jakarta.mail.MessagingException e) {
            redisUtil.delete(codeKey(purpose, target));
            redisUtil.delete(resendKey(purpose, target));
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILED);
        }

        log.info("이메일 인증 코드(REDIS) 저장: email={}, ttl={}s", target, CODE_TTL.toSeconds());
    }

    public void verifyEmailCode(AuthRequestDTO.EmailVerifyRequestDTO request) {
        String target = request.email().toLowerCase();
        Objects.requireNonNull(request.code(), "code는 null일 수 없습니다.");
        String purpose = VerificationPurpose.EMAIL_VERIFY.name();

        String codeKey = codeKey(purpose, target);
        String storedHashed = redisUtil.get(codeKey);
        if (storedHashed == null) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_EXPIRED);
        }

        long tries = redisUtil.incrWithTtl(triesKey(purpose, target), CODE_TTL);
        if (tries > MAX_TRIES) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_TRY_EXCEEDED);
        }

        String inputHashed = hasher.hash(request.code());
        if (!storedHashed.equals(inputHashed)) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID);
        }

        var user = userClient.byEmail(target);
        userClient.markEmailVerified(user.id());

        redisUtil.delete(codeKey);
        redisUtil.delete(triesKey(purpose, target));
        redisUtil.delete(resendKey(purpose, target));

        log.info("이메일 인증 완료(REDIS): email={}", target);
    }

    public void startEmailChange(UUID userId, String newEmail) {
        String target = newEmail.toLowerCase();
        var u = userClient.byId(userId);

        if (u.email().equalsIgnoreCase(target)) throw new AuthException(AuthErrorCode.EMAIL_SAME_AS_OLD);
        if (userClient.existsByEmailBool(target)) throw new AuthException(AuthErrorCode.EMAIL_IN_USE);

        userClient.startEmailChange(userId, target);

        String purpose = VerificationPurpose.CHANGE_EMAIL.name();

        if (!redisUtil.setIfAbsent(resendKey(purpose, target), "1", RESEND_TTL)) {
            throw new AuthException(AuthErrorCode.EMAIL_RESEND_COOLDOWN);
        }

        String code = generateCode(CODE_LEN);
        String hashed = hasher.hash(code);

        redisUtil.setWithTtl(codeKey(purpose, target), hashed, CODE_TTL);
        redisUtil.delete(triesKey(purpose, target));

        String title = "이메일 변경 인증 번호";
        String content = """
                <html><body>
                <h1>인증 코드 : %s</h1>
                <p>해당 코드를 홈페이지에 입력하세요.</p>
                <p>* 본 메일은 자동응답 메일입니다.</p>
                </body></html>
                """.formatted(code);
        try {
            emailService.sendSimpleMessage(target, title, content);
        } catch (RuntimeException | jakarta.mail.MessagingException e) {
            redisUtil.delete(codeKey(purpose, target));
            redisUtil.delete(resendKey(purpose, target));
            throw new AuthException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }

    public void verifyEmailChange(UUID userId, String newEmail, String code) {
        String target = newEmail.toLowerCase();
        if (userClient.existsByEmailBool(target)) throw new AuthException(AuthErrorCode.EMAIL_IN_USE);

        String purpose = VerificationPurpose.CHANGE_EMAIL.name();

        String kCode = codeKey(purpose, target);
        String storedHashed = redisUtil.get(kCode);
        if (storedHashed == null) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_EXPIRED);
        }

        long tries = redisUtil.incrWithTtl(triesKey(purpose, target), CODE_TTL);
        if (tries > MAX_TRIES) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_TRY_EXCEEDED);
        }

        String inputHashed = hasher.hash(code);
        if (!storedHashed.equals(inputHashed)) {
            throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID);
        }

        userClient.confirmEmailChange(userId, target);

        redisUtil.delete(kCode);
        redisUtil.delete(triesKey(purpose, target));
        redisUtil.delete(resendKey(purpose, target));

        log.info("이메일 변경 확정(REDIS): userId={}, newEmail={}", userId, target);
    }

    private String generateCode(int len) {
        var r = new Random();
        var sb = new StringBuilder();
        for (int i = 0; i < len; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

    private String stripBearer(String value) {
        return value != null && value.startsWith("Bearer ") ? value.substring(7) : value;
    }
}