package com.fourthread.ozang.module.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
    log.error("Unhandled exception occurred", e);

    ErrorDetails details = new ErrorDetails(
        e.getClass().getSimpleName(),
        e.getMessage()
    );

    ErrorResponse errorResponse = new ErrorResponse(
        ErrorCode.INTERNAL_SERVER_ERROR.name(),
        ErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
        details);

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }

  @ExceptionHandler(GlobalException.class)
  public ResponseEntity<ErrorResponse> handleBaseException(GlobalException e) {
    ErrorResponse errorResponse = new ErrorResponse(
        e.getExceptionName(),
        e.getMessage(),
        e.getDetails()
    );

    return ResponseEntity.badRequest().body(errorResponse);
  }
}
