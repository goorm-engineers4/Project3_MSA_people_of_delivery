package com.example.cloudfour.userservice.domain.user.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum UserAddressErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "USERADDRESS400_1", "회원 주소 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "USERADDRESS400_2", "회원 주소 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "USERADDRESS400_3", "회원 주소 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "USERADDRESS401", "회원 주소에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "USERADDRESS404", "회원 주소를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "USERADDRESS409", "이미 등록된 회원 주소입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "USERADDRESS500", "회원 주소 처리 중 서버 오류가 발생했습니다."),
    PARSE_FAILED(HttpStatus.BAD_REQUEST, "USERADDRESS400_4", "주소 파싱에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
