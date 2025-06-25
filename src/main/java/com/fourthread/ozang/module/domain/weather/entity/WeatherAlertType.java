package com.fourthread.ozang.module.domain.weather.entity;

public enum WeatherAlertType {
    TEMPERATURE_RISE("기온 상승"),
    TEMPERATURE_DROP("기온 하강"),
    PRECIPITATION_START("강수 시작"),
    PRECIPITATION_CHANGE("강수 형태 변화");

    private final String description;

    WeatherAlertType(String description) {
        this.description = description;
    }

    public String getDescription() { return description; }
}