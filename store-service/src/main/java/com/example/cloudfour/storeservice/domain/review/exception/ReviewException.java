package com.example.cloudfour.storeservice.domain.review.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class ReviewException extends CustomException {
    public ReviewException(BaseErrorCode errorCode) {
      super(errorCode);
    }
}
