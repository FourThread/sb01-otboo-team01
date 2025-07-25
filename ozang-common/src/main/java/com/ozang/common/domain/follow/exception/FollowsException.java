package com.ozang.common.domain.follow.exception;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class FollowsException extends GlobalException {
    public FollowsException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
