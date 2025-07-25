package com.ozang.common.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ozang.common.domain.weather.dto.type.SkyStatus;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record WeatherSummaryDto(
     UUID weatherId,
     SkyStatus skyStatus,
     PrecipitationDto precipitation,
     TemperatureDto temperature
) {}