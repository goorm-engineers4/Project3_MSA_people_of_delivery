package com.example.cloudfour.userservice.domain.user.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum UserErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "USER400_1", "회원 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "USER400_2", "회원 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "USER400_3", "회원 정보를 삭제할 수 없습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER400_4", "현재 비밀번호가 일치하지 않습니다."),
    EMAIL_SAME_AS_OLD(HttpStatus.BAD_REQUEST, "USER400_5", "기존 이메일과 동일합니다."),
    PENDING_EMAIL_MISMATCH(HttpStatus.BAD_REQUEST, "USER400_6", "변경 대기 중인 이메일이 일치하지 않습니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "USER400_7", "요청 값이 올바르지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "USER401", "회원에 접근할 수 있는 권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404", "회원을 찾을 수 없습니다."),
    EMAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404_1", "해당 이메일의 사용자를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "USER409", "이미 등록된 회원입니다."),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "USER409_1", "이미 사용 중인 이메일입니다."),
    ALREADY_VERIFIED(HttpStatus.CONFLICT, "USER409_2", "이미 이메일 인증이 완료된 계정입니다."),
    CONFLICT_STATE(HttpStatus.CONFLICT, "USER409_3", "요청 처리 중 상태 충돌이 발생했습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "USER500", "회원 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
