package com.example.cloudfour.storeservice.domain.store.exception;


import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class StoreException extends CustomException {
  public StoreException(BaseErrorCode errorCode) {
    super(errorCode);
  }
}
