package com.fourthread.ozang.core.domain.weather.dto;


import com.fourthread.ozang.core.domain.weather.dto.type.PrecipitationType;

/**
 * 강수 정보
 */
public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    Double probability
) {}