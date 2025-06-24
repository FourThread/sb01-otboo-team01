package com.fourthread.ozang.module.domain.weather.dto;


import com.fourthread.ozang.module.domain.weather.entity.PrecipitationType;

public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    Double probability
) {}