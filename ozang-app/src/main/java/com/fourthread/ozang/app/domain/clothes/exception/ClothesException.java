package com.fourthread.ozang.app.domain.clothes.exception;

import com.fourthread.ozang.core.common.exception.ErrorCode;
import com.fourthread.ozang.core.common.exception.ErrorDetails;
import com.fourthread.ozang.core.common.exception.GlobalException;

public class ClothesException extends GlobalException {
    public ClothesException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
