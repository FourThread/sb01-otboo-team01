package com.fourthread.ozang.domain.notification.execption;

import com.fourthread.ozang.common.exception.ErrorCode;
import com.fourthread.ozang.common.exception.ErrorDetails;
import com.fourthread.ozang.common.exception.GlobalException;

public class NotificationException extends GlobalException {
    public NotificationException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
