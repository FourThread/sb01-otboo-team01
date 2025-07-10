package com.fourthread.ozang.module.domain.weather.dto;


import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;

/**
 * 강수 정보
 */
public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    Double probability
) {

    public static PrecipitationDto of(PrecipitationType type,
        double amount, double probability) {
        return new PrecipitationDto(type, amount,
            probability);
    }
}