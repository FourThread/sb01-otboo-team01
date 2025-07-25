package com.fourthread.ozang.domain.clothes.exception;

import com.fourthread.ozang.common.exception.ErrorCode;
import com.fourthread.ozang.common.exception.ErrorDetails;
import com.fourthread.ozang.common.exception.GlobalException;

public class ClothesAttributeDefinitionException extends GlobalException {
    public ClothesAttributeDefinitionException(ErrorCode errorCode, String exceptionClass, String exceptionMessage) {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
