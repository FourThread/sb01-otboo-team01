package com.ozang.common.domain.clothes.exception;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class ClothesAttributeDefinitionException extends GlobalException {
    public ClothesAttributeDefinitionException(ErrorCode errorCode, String exceptionClass, String exceptionMessage) {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
