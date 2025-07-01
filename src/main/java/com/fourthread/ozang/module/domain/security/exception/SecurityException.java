package com.fourthread.ozang.module.domain.security.exception;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class SecurityException extends GlobalException {

  public SecurityException(String exceptionName, String message,
      ErrorDetails details) {
    super(exceptionName, message, details);
  }
}
