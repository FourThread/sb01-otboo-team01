package com.fourthread.ozang.module.domain.weather.dto;

import com.fourthread.ozang.module.domain.weather.entity.WindSpeedLevel;

public record WindSpeedDto(
     Double speed,
     WindSpeedLevel asWord
) {}