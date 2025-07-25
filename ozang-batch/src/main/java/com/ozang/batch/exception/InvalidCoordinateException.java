package com.ozang.batch.exception;

public class InvalidCoordinateException extends RuntimeException {

    public InvalidCoordinateException(String message) {
        super(message);
    }

    public InvalidCoordinateException() {
        super("잘못된 좌표입니다.");
    }
}
