package com.fourthread.ozang.module.domain.weather.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 기상청 API 최상위 응답
 */
public record WeatherApiResponse(
    @JsonProperty("response") WeatherApiResponseBody response
) {}

/**
 * 기상청 API 응답 본문
 */
record WeatherApiResponseBody(
    @JsonProperty("header") WeatherApiHeader header,
    @JsonProperty("body") WeatherApiBody body
) {}

/**
 * 기상청 API 본문
 */
record WeatherApiBody(
    @JsonProperty("dataType") String dataType,
    @JsonProperty("items") WeatherApiItems items,
    @JsonProperty("pageNo") Integer pageNo,
    @JsonProperty("numOfRows") Integer numOfRows,
    @JsonProperty("totalCount") Integer totalCount
) {}

/**
 * 기상청 API 아이템들
 */
record WeatherApiItems(
    @JsonProperty("item") List<WeatherApiItem> item
) {}

/**
 * 기상청 API 개별 아이템
 */
record WeatherApiItem(
    @JsonProperty("baseDate") String baseDate,
    @JsonProperty("baseTime") String baseTime,
    @JsonProperty("category") String category,
    @JsonProperty("fcstDate") String fcstDate,
    @JsonProperty("fcstTime") String fcstTime,
    @JsonProperty("fcstValue") String fcstValue,
    @JsonProperty("nx") Integer nx,
    @JsonProperty("ny") Integer ny
) {}

