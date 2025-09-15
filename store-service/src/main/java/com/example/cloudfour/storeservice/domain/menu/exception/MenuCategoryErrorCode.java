package com.example.cloudfour.storeservice.domain.menu.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum MenuCategoryErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "MENUCATEGORY400_1", "메뉴 카테고리 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "MENUCATEGORY400_2", "메뉴 카테고리 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "MENUCATEGORY400_3", "메뉴 카테고리 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "MENUCATEGORY401", "메뉴 카테고리에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "MENUCATEGORY404", "메뉴 카테고리를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "MENUCATEGORY409", "이미 등록된 메뉴 카테고리입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "MENUCATEGORY500", "메뉴 카테고리 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
