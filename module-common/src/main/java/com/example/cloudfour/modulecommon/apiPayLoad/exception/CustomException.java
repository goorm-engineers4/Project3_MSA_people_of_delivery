package com.example.cloudfour.modulecommon.apiPayLoad.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException {
    private final BaseErrorCode code;

    public CustomException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage); // 사용자 지정 메시지
        this.code = errorCode;
    }

    @Override
    public String getMessage() {
        return code.getMessage();
    }
}
