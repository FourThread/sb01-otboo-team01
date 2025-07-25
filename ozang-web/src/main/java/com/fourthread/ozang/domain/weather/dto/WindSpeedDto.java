package com.fourthread.ozang.domain.weather.dto;

import com.fourthread.ozang.domain.weather.dto.type.WindStrength;

public record WindSpeedDto(
     Double speed,
     WindStrength asWord
) {}