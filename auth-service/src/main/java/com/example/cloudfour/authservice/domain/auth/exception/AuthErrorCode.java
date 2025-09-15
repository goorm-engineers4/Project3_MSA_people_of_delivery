package com.example.cloudfour.authservice.domain.auth.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum AuthErrorCode implements BaseErrorCode {
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "AUTH409_1", "이미 가입된 이메일입니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_1", "존재하지 않는 이메일입니다."),
    PASSWORD_INVALID(HttpStatus.BAD_REQUEST, "AUTH400_1", "아이디 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_LOGIN_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "AUTH400_2", "소셜 계정은 로컬 로그인 불가"),
    ACCOUNT_DELETED(HttpStatus.BAD_REQUEST, "AUTH400_3", "이미 탈퇴한 계정입니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "AUTH400_4", "이메일 인증이 필요합니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404_2", "사용자를 찾을 수 없습니다."),
    EMAIL_CODE_INVALID(HttpStatus.BAD_REQUEST, "AUTH400_5", "정확하지 않은 인증코드입니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH400_6", "만료된 인증코드입니다."),
    EMAIL_IN_USE(HttpStatus.CONFLICT, "AUTH409_2", "이미 사용 중인 이메일입니다."),
    EMAIL_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "AUTH400_7", "기존 이메일과 동일합니다."),
    EMAIL_CHANGE_PENDING_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH400_8", "변경 대기 중인 이메일이 일치하지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH500_1", "이메일 발송 실패"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH400_9", "현재 비밀번호가 일치하지 않습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_012", "유효하지 않은 토큰입니다."),
    REFRESH_NOT_MATCHED(HttpStatus.UNAUTHORIZED, "AUTH_013", "리프레시 토큰이 일치하지 않습니다."),
    TOKEN_TYPE_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_014", "토큰 타입이 올바르지 않습니다."),
    EMAIL_RESEND_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "AUTH006", "인증 코드 재전송은 잠시 후 다시 시도해주세요."),
    EMAIL_CODE_TRY_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AUTH007", "인증 코드 시도 횟수를 초과했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}


