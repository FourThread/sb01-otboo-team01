package com.ozang.common.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
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