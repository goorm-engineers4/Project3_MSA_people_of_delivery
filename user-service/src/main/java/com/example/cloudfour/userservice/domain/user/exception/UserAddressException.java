package com.example.cloudfour.userservice.domain.user.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class UserAddressException extends CustomException {
    public UserAddressException(BaseErrorCode errorCode) {
      super(errorCode);
    }
}
