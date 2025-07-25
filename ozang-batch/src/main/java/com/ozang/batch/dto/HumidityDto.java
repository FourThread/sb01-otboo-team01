package com.ozang.batch.dto;

public record HumidityDto(
    Double current,
    Double comparedToDayBefore
) {}