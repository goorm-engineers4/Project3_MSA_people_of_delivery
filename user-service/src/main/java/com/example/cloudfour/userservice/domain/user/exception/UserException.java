package com.example.cloudfour.userservice.domain.user.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class UserException extends CustomException {
    public UserException(BaseErrorCode errorCode) {
      super(errorCode);
    }
}
