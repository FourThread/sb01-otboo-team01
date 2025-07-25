package com.fourthread.ozang.module.domain.weather.dto.type;

public enum WeatherChangeType {
    TEMPERATURE_RISE("급격한 온도 상승"),
    TEMPERATURE_DROP("급격한 온도 하강"),
    PRECIPITATION_START("강수 시작"),
    PRECIPITATION_END("강수 종료"),
    PRECIPITATION_TYPE_CHANGE("강수 형태 변경"),
    WIND_INCREASE("강풍 발생"),
    SKY_CHANGE("하늘 상태 급변");

    private final String description;

    WeatherChangeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}