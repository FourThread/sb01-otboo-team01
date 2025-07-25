package com.fourthread.ozang.module.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_EMPTY)
public record ErrorResponse(
    String exceptionName,
    String message,
    ErrorDetails details
) {

}
