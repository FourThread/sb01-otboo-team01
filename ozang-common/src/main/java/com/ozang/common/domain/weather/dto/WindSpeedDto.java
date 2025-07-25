package com.ozang.common.domain.weather.dto;

import com.ozang.common.domain.weather.dto.type.WindStrength;

public record WindSpeedDto(
     Double speed,
     WindStrength asWord
) {}