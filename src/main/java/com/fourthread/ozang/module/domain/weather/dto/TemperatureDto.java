package com.fourthread.ozang.module.domain.weather.dto;

public record TemperatureDto(
    Double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {

    public static TemperatureDto of(double temperature, double comparedToDayBefore, double min, double max) {
        return new TemperatureDto(temperature, comparedToDayBefore, min, max);
    }
}
