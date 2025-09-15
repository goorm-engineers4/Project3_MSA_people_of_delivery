package com.example.cloudfour.storeservice.domain.menu.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class MenuException extends CustomException {
    public MenuException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
