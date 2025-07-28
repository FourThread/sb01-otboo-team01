package com.fourthread.ozang.core.domain.weather.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {}