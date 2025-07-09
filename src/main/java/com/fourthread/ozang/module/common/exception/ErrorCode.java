package com.fourthread.ozang.module.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
  // 공통
  INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_REQUEST("INVALID_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),

  // 사용자 관련
  USER_NOT_FOUND("USER_NOT_FOUND", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  USERNAME_ALREADY_EXISTS("USERNAME_ALREADY_EXISTS", "이미 사용 중인 사용자 이름입니다.", HttpStatus.CONFLICT),
  EMAIL_ALREADY_EXISTS("EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
  PROFILE_NOT_FOUND("PROFILE_NOT_FOUND", "프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  TEMP_PASSWORD_EXPIRED("TEMP_PASSWORD_EXPIRED", "임시 비밀번호가 만료되었습니다.", HttpStatus.UNAUTHORIZED),
  
  // 피드 관련
  FEED_NOT_FOUND("Feed Not Found", "존재하지 않는 피드입니다", HttpStatus.NOT_FOUND),
  FEED_LIKE_NOT_FOUND("Feed Like Not Found", "피드를 좋아요 하지 않았습니다", HttpStatus.NOT_FOUND),

  // 의상 관련
  CLOTHES_NOT_FOUND("CLOTHES_NOT_FOUND", "의상 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  DUPLICATE_CLOTHES("DUPLICATE_CLOTHES", "이미 존재하는 의상입니다.", HttpStatus.CONFLICT),
  CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND("CLOTHES_ATTRIBUTE_DEFINITION_NOT_FOUND", "의상 속성 정의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION("DUPLICATE_CLOTHES_ATTRIBUTE_DEFINITION", "이미 존재하는 의상 속성 정의입니다.", HttpStatus.CONFLICT),

  FILE_UPLOAD_ERROR("FILE_UPLOAD_ERROR", "파일 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  FILE_SIZE_EXCEEDED( "FILE_SIZE_EXCEEDED","파일 크기가 제한을 초과했습니다.",  HttpStatus.BAD_REQUEST),
  INVALID_FILE_TYPE( "INVALID_FILE_TYPE", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
  FILE_DELETE_ERROR( "FILE_DELETE_ERROR", "파일 삭제 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

  private final String code;
  private final String message;
  private final HttpStatus httpStatus;

  ErrorCode(String code, String message, HttpStatus httpStatus) {
    this.code = code;
    this.message = message;
    this.httpStatus = httpStatus;
  }
}
