package com.fourthread.ozang.module.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

  FEED_NOT_FOUND("Feed Not Found", "존재하지 않는 피드입니다"),
  FEED_LIKE_NOT_FOUND("Feed Like Not Found", "피드를 좋아요 하지 않았습니다");

  private final String exceptionName;
  private final String message;

  ErrorCode(String exceptionName, String message) {
    this.exceptionName = exceptionName;
    this.message = message;
  }
}
