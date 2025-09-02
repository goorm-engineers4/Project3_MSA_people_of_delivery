package com.example.cloudfour.storeservice.domain.menu.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum MenuErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "MENU400_1", "메뉴 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "MENU400_2", "메뉴 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "MENU400_3", "메뉴 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "MENU401", "메뉴에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "MENU404", "메뉴를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "MENU409", "이미 등록된 메뉴입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MENU500", "메뉴 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
