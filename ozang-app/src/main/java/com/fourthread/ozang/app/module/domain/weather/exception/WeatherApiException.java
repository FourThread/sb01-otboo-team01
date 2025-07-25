package com.fourthread.ozang.module.domain.weather.exception;

public class WeatherApiException extends RuntimeException {

    private final String resultCode;

    public WeatherApiException(String message, String resultCode) {
        super(message);
        this.resultCode = resultCode;
    }

    public WeatherApiException(String message, String resultCode, Throwable cause,
        String resultCode1) {
        super(message, cause);
        this.resultCode = resultCode1;
    }

    public String getResultCode() {
        return resultCode;
    }
}