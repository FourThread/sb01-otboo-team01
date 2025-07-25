package com.ozang.common.domain.weather.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {}