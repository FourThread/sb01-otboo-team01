package com.fourthread.ozang.module.common.exception;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnhandledException(Exception e, HttpServletRequest request) {
    if (e instanceof AccessDeniedException) {
      throw (AccessDeniedException) e;
    }

    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("text/event-stream")) {
      return ResponseEntity.noContent().build(); // SSE 요청은 JSON 응답 안 함
    }

    log.error("Unhandled exception occurred", e);

    String accept = request.getHeader("Accept");
    if (accept != null && accept.contains("text/event-stream")) {
      return ResponseEntity.noContent().build(); // SSE 요청은 JSON 응답 안 함
    }

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

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
    BindingResult bindingResult = e.getBindingResult();

    FieldError fieldError = bindingResult.getFieldError();
    String errorMessage = fieldError != null ? fieldError.getDefaultMessage() : "유효하지 않은 요청입니다.";

    ErrorDetails details = new ErrorDetails(
            e.getClass().getSimpleName(),
            errorMessage
    );

    ErrorResponse errorResponse = new ErrorResponse(
            ErrorCode.BAD_REQUEST.name(),
            errorMessage,
            details
    );

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
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
