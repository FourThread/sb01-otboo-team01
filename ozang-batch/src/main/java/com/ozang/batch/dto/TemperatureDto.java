package com.fourthread.ozang.module.domain.weather.dto;

public record TemperatureDto(
    Double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {}
