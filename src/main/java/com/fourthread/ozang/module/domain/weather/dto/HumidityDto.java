package com.fourthread.ozang.module.domain.weather.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {

    public static HumidityDto of(double current, double comparedToDayBefore) {
        return new HumidityDto(current, comparedToDayBefore);
    }
}