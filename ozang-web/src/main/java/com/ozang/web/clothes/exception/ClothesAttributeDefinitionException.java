package com.ozang.web.clothes.exception;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.common.exception.GlobalException;

public class ClothesAttributeDefinitionException extends GlobalException {
    public ClothesAttributeDefinitionException(ErrorCode errorCode, String exceptionClass, String exceptionMessage) {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
