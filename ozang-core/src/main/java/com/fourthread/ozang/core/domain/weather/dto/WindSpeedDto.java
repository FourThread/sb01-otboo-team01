package com.fourthread.ozang.core.domain.weather.dto;

import com.fourthread.ozang.core.domain.weather.dto.type.WindStrength;

public record WindSpeedDto(
     Double speed,
     WindStrength asWord
) {}