package com.fourthread.ozang.module.domain.weather.dto;

import com.fourthread.ozang.module.domain.weather.entity.SkyStatus;
import java.util.UUID;

public record WeatherSummaryDto(
     UUID weatherId,
     SkyStatus skyStatus,
     PrecipitationDto precipitation,
     TemperatureDto temperature
) {}