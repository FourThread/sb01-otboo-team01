package com.ozang.batch.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 기상청 API 헤더
 */
public record WeatherApiHeader(
    @JsonProperty("resultCode") String resultCode,
    @JsonProperty("resultMsg") String resultMsg
) {}