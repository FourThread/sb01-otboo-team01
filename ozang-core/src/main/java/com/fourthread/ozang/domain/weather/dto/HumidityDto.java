package com.fourthread.ozang.domain.weather.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {}