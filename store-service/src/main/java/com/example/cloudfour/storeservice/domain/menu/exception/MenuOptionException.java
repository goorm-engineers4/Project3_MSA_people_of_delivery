package com.example.cloudfour.storeservice.domain.menu.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class MenuOptionException extends CustomException {
    public MenuOptionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}
