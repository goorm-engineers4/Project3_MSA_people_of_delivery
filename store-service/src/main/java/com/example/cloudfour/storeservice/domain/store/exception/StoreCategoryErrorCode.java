package com.example.cloudfour.storeservice.domain.store.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum StoreCategoryErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "STORECATEGORY400_1", "가게 카테고리 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "STORECATEGORY400_2", "가게 카테고리 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "STORECATEGORY400_3", "가게 카테고리 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "STORECATEGORY401", "가게 카테고리에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "STORECATEGORY404", "가게 카테고리를 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "STORECATEGORY409", "이미 등록된 가게 카테고리입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "STORECATEGORY500", "가게 카테고리 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
