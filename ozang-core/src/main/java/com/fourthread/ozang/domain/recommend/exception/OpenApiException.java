package com.fourthread.ozang.domain.recommend.exception;

import com.fourthread.ozang.common.exception.ErrorDetails;
import com.fourthread.ozang.common.exception.GlobalException;

public class OpenApiException extends GlobalException {


  public OpenApiException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }

}
