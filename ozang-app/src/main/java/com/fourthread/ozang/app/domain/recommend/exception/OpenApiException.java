package com.fourthread.ozang.app.domain.recommend.exception;

import com.fourthread.ozang.app.common.exception.ErrorDetails;
import com.fourthread.ozang.app.common.exception.GlobalException;

public class OpenApiException extends GlobalException {


  public OpenApiException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }

}
