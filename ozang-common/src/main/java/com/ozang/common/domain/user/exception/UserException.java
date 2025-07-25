package com.fourthread.ozang.module.domain.user.exception;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class UserException extends GlobalException {
  public UserException(ErrorCode errorCode, String debugMessage, String source) {
    super(
        errorCode.getCode(),
        errorCode.getMessage(),
        new ErrorDetails(source, debugMessage)
    );
  }
}
