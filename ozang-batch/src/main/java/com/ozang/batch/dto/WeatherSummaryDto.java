package com.ozang.batch.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ozang.batch.dto.type.SkyStatus;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record WeatherSummaryDto(
     UUID weatherId,
     SkyStatus skyStatus,
     PrecipitationDto precipitation,
     TemperatureDto temperature
) {}