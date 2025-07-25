package com.ozang.batch.dto;

public record TemperatureDto(
    Double current,
    Double comparedToDayBefore,
    Double min,
    Double max
) {}
