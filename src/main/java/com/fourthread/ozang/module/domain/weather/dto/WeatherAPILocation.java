package com.fourthread.ozang.module.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WeatherAPILocation(
    @JsonProperty("latitude") Double latitude,
    @JsonProperty("longitude") Double longitude,
    @JsonProperty("x") Integer x,
    @JsonProperty("y") Integer y,
    @JsonProperty("locationNames") List<String> locationNames
) {

    @JsonCreator
    public WeatherAPILocation {
    }
}