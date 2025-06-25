package com.fourthread.ozang.module.common.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {

  private final String exceptionName;
  private final String message;
  private final ErrorDetails details;

  public GlobalException(String exceptionName, String message, ErrorDetails details) {
    super(message);
    this.exceptionName = exceptionName;
    this.message = message;
    this.details = details;
  }
}