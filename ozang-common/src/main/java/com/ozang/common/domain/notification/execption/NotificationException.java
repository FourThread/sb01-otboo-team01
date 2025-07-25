package com.ozang.common.domain.notification.execption;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class NotificationException extends GlobalException {
    public NotificationException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
