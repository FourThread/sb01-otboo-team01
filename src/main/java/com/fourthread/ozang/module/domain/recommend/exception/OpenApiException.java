package com.fourthread.ozang.module.domain.recommend.exception;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class OpenApiException extends GlobalException {


  public OpenApiException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }

}
