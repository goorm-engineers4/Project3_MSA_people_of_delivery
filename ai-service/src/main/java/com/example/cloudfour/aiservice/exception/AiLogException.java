package com.example.cloudfour.aiservice.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class AiLogException extends CustomException {
    public AiLogException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
