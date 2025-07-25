package com.fourthread.ozang.domain.weather.dto;

public record TemperatureDto(
    Double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {}
