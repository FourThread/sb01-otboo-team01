package com.fourthread.ozang.module.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
  INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
  INVALID_REQUEST("잘못된 요청입니다.");

  private final String message;

  ErrorCode(String message) {this.message = message;}
}
