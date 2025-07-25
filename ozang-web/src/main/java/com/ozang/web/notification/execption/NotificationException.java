package com.ozang.web.notification.execption;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class NotificationException extends GlobalException {
    public NotificationException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
