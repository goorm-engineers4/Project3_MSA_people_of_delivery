package com.example.cloudfour.storeservice.domain.store.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class StoreCategoryException extends CustomException {
    public StoreCategoryException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
