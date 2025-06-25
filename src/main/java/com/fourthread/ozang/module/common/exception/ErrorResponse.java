package com.fourthread.ozang.module.common.exception;

public record ErrorResponse(

    String exceptionName,
    String message,
    ErrorDetails details

) {

}
