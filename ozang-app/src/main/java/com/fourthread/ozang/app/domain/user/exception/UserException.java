package com.fourthread.ozang.app.domain.user.exception;

import com.fourthread.ozang.core.common.exception.ErrorCode;
import com.fourthread.ozang.core.common.exception.ErrorDetails;
import com.fourthread.ozang.core.common.exception.GlobalException;

public class UserException extends GlobalException {
  public UserException(ErrorCode errorCode, String debugMessage, String source) {
    super(
        errorCode.getCode(),
        errorCode.getMessage(),
        new ErrorDetails(source, debugMessage)
    );
  }
}
