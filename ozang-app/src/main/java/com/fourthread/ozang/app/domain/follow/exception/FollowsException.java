package com.fourthread.ozang.app.domain.follow.exception;

import com.fourthread.ozang.core.common.exception.ErrorCode;
import com.fourthread.ozang.core.common.exception.ErrorDetails;
import com.fourthread.ozang.core.common.exception.GlobalException;

public class FollowsException extends GlobalException {
    public FollowsException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
