package com.fourthread.ozang.module.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  FEED_NOT_FOUND("Feed Not Found", "존재하지 않는 피드");

  private final String exceptionName;
  private final String message;

  ErrorCode(String exceptionName, String message) {
    this.exceptionName = exceptionName;
    this.message = message;
  }
}
