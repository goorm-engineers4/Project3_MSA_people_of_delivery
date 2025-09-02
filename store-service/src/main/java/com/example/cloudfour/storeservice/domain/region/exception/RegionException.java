package com.example.cloudfour.storeservice.domain.region.exception;

import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;

public class RegionException extends CustomException {
    public RegionException(BaseErrorCode errorCode) {
        super(errorCode);
    }
}