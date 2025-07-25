package com.ozang.common.domain.clothes.exception;

import com.ozang.common.exception.ErrorCode;
import com.ozang.common.exception.ErrorDetails;
import com.ozang.common.exception.GlobalException;

public class ClothesException extends GlobalException {
    public ClothesException(ErrorCode errorCode, String exceptionClass, String exceptionMessage)  {
        super(
                errorCode.getCode(),
                errorCode.getMessage(),
                new ErrorDetails(exceptionClass, exceptionMessage));
    }
}
