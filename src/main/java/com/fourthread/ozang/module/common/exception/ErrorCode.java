package com.fourthread.ozang.module.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다."),
  INVALID_REQUEST("잘못된 요청입니다."),

  //User
  USER_NOT_FOUND("사용자를 찾을 수 없습니다"),
  USERNAME_ALREADY_EXISTS("이미 사용 중인 사용자 이름입니다."),
  EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다."),
  PROFILE_NOT_FOUND("프로필을 찾을 수 없습니다");

  private final String message;

  ErrorCode(String message) {this.message = message;}
}
