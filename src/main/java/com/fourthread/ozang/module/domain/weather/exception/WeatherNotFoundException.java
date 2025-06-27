package com.fourthread.ozang.module.domain.weather.exception;

public class WeatherNotFoundException extends RuntimeException {

    public WeatherNotFoundException(String message) {
        super(message);
    }

    public WeatherNotFoundException() {
        super("날씨 정보를 찾을 수 없습니다.");
    }
}