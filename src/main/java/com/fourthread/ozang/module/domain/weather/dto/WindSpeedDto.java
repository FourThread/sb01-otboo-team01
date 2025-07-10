package com.fourthread.ozang.module.domain.weather.dto;

import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;

public record WindSpeedDto(
     Double speed,
     WindStrength asWord
) {

    public static WindSpeedDto of(double windSpeed, WindStrength windStrength) {
        return new WindSpeedDto(windSpeed, windStrength);
    }
}