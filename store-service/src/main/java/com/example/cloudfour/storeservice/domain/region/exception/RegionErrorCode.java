package com.example.cloudfour.storeservice.domain.region.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum RegionErrorCode implements BaseErrorCode {
    CREATE_FAILED(HttpStatus.BAD_REQUEST, "REGION400_1", "지역 정보를 생성할 수 없습니다."),
    UPDATE_FAILED(HttpStatus.BAD_REQUEST, "REGION400_2", "지역 정보를 수정할 수 없습니다."),
    DELETE_FAILED(HttpStatus.BAD_REQUEST, "REGION400_3", "지역 정보를 삭제할 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.UNAUTHORIZED, "REGION401", "지역에 접근할 수 있는 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "REGION404", "지역을 찾을 수 없습니다."),
    ALREADY_ADD(HttpStatus.CONFLICT, "REGION409", "이미 등록된 지역입니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REGION500", "지역 처리 중 서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
