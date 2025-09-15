package com.example.cloudfour.modulecommon.apiPayLoad.exception.handler;

import com.example.cloudfour.modulecommon.apiPayLoad.CustomResponse;
import com.example.cloudfour.modulecommon.apiPayLoad.code.BaseErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.code.GeneralErrorCode;
import com.example.cloudfour.modulecommon.apiPayLoad.exception.CustomException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(CustomException.class)
  public ResponseEntity<CustomResponse<String>> handleCustomException(CustomException ex) {
    log.error("[ CustomException ]: {} ", ex.getCode().getMessage());
    BaseErrorCode errorCode = ex.getCode();
    CustomResponse<String> errorResponse = CustomResponse.onFailure(errorCode.getCode(), ex.getMessage());
    return ResponseEntity.status(ex.getCode().getStatus()).body(errorResponse);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<CustomResponse<List<String>>> handleConstraintViolationException(ConstraintViolationException ex) {
    log.error(Arrays.toString(ex.getStackTrace()));
    List<String> message = ex.getConstraintViolations().stream().map(ConstraintViolation::getMessage).toList();
    log.error("[ ConstraintViolationException ]: {} ", message);
    BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST_400;
    CustomResponse<List<String>> errorResponse = CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage(),null);

    return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<CustomResponse<Map<String,String>>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
    Map<String, String> error = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(fieldError -> error.put(fieldError.getField(), fieldError.getDefaultMessage()));
    BaseErrorCode errorCode = GeneralErrorCode.BAD_REQUEST_400;
    log.error("[ MethodArgumentNotValidException ]: {} ", error);
    CustomResponse<Map<String,String>> errorResponse = CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage(),error);
    return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<CustomResponse<Map<String,String>>> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
    log.error("[ DataIntegrityViolationException ]: {} ", ex.getMessage());

    BaseErrorCode errorCode = GeneralErrorCode.DUPLICATE_409;
    CustomResponse<Map<String, String>> errorResponse = CustomResponse.onFailure(errorCode.getCode(), errorCode.getMessage());

    return ResponseEntity.status(errorCode.getStatus())
            .body(errorResponse);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<CustomResponse<String>> handleException(Exception ex) {
    log.error("[WARNING] Internal Server Error: {}", ex.getMessage());
    BaseErrorCode errorCode = GeneralErrorCode.INTERNAL_SERVER_ERROR_500;
    CustomResponse<String> errorResponse = CustomResponse.onFailure(errorCode.getCode(), ex.getMessage());
    return ResponseEntity.status(errorCode.getStatus()).body(errorResponse);
  }
}
