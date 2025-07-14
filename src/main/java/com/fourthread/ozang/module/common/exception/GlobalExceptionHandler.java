package com.fourthread.ozang.module.common.exception;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e) {
    if (e instanceof AccessDeniedException) {
      throw (AccessDeniedException) e;
    }
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

  @ExceptionHandler(MissingRequestCookieException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(MissingRequestCookieException e) {
    String cookieName = e.getCookieName();
    log.warn("[GlobalExceptionHandler] Missing required cookie: {}", cookieName);

    String userFriendlyMessage;
    if ("refresh_token".equals(cookieName)) {
      userFriendlyMessage = "로그인이 필요합니다. 다시 로그인해 주세요.";
    } else {
      userFriendlyMessage = "필수 쿠키가 누락되었습니다: " + cookieName;
    }

    ErrorDetails details = new ErrorDetails(
        "MissingRequestCookieException",
        "Cookie: " + cookieName + " | Suggestion: Please login again to get the required authentication cookie"
    );

    ErrorResponse errorResponse = new ErrorResponse(
        "MissingRequestCookieException",
        userFriendlyMessage,
        details
    );

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
  }
}
