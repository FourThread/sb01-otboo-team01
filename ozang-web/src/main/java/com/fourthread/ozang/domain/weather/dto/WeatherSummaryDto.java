package com.fourthread.ozang.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record WeatherSummaryDto(
     UUID weatherId,
     SkyStatus skyStatus,
     PrecipitationDto precipitation,
     TemperatureDto temperature
) {}