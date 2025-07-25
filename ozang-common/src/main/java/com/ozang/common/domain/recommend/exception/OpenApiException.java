package com.ozang.common.domain.recommend.exception;

import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class OpenApiException extends GlobalException {


  public OpenApiException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }

}
