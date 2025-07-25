package com.ozang.common.domain.user.exception;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class UserException extends GlobalException {
  public UserException(ErrorCode errorCode, String debugMessage, String source) {
    super(
        errorCode.getCode(),
        errorCode.getMessage(),
        new ErrorDetails(source, debugMessage)
    );
  }
}
