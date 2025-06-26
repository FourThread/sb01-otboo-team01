package com.fourthread.ozang.module.domain.weather.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {}