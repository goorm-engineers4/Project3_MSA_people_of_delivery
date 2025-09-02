package com.example.cloudfour.aiservice.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiLogErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "AILOG400_1", "AI 로그 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "AILOG400_2", "AI 로그 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "AILOG400_3", "AI 로그 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "AILOG401", "AI 로그에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "AILOG404", "AI 로그를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "AILOG409", "이미 등록된 AI 로그입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AILOG500", "AI 로그 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}