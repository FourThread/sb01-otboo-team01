package com.fourthread.ozang.module.domain.user.exception;

import com.fourthread.ozang.module.common.exception.BaseException;
import com.fourthread.ozang.module.common.exception.ErrorCode;
import java.util.Map;

public class UserException extends BaseException {

  public UserException(ErrorCode errorCode) {
    super(errorCode);
  }

  public UserException(ErrorCode errorCode, Map<String, Object> details) {
    super(errorCode, details);
  }
}
