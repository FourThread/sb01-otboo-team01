package com.fourthread.ozang.module.domain.weather.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 기상청 단기예보 API 응답 DTO
 */

public record WeatherApiResponse(
    @JsonProperty("response") ResponseBody response
) {

    public record ResponseBody(
        @JsonProperty("header") Header header,
        @JsonProperty("body") Body body
    ) {}

    public record Header(
        @JsonProperty("resultCode") String resultCode,
        @JsonProperty("resultMsg") String resultMsg
    ) {}

    public record Body(
        @JsonProperty("dataType") String dataType,
        @JsonProperty("items") Items items,
        @JsonProperty("pageNo") Integer pageNo,
        @JsonProperty("numOfRows") Integer numOfRows,
        @JsonProperty("totalCount") Integer totalCount
    ) {}

    public record Items(
        @JsonProperty("item") List<Item> item
    ) {}

    public record Item(
        @JsonProperty("baseDate") String baseDate,
        @JsonProperty("baseTime") String baseTime,
        @JsonProperty("category") String category,
        @JsonProperty("fcstDate") String fcstDate,
        @JsonProperty("fcstTime") String fcstTime,
        @JsonProperty("fcstValue") String fcstValue,
        @JsonProperty("nx") Integer nx,
        @JsonProperty("ny") Integer ny
    ) {}
}