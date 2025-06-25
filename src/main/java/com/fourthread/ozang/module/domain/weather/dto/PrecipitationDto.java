package com.fourthread.ozang.module.domain.weather.dto;


import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;

public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    Double probability
) {}