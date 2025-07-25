package com.ozang.batch.dto;


import com.ozang.batch.dto.type.PrecipitationType;

/**
 * 강수 정보
 */
public record PrecipitationDto(
    PrecipitationType type,
    Double amount,
    Double probability
) {}