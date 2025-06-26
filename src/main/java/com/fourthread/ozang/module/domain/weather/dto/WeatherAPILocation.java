package com.fourthread.ozang.module.domain.weather.dto;

import java.util.List;

public record WeatherAPILocation(
     Double latitude,
     Double longitude,
     Integer x,
     Integer y,
     List<String> locationNames
) {}