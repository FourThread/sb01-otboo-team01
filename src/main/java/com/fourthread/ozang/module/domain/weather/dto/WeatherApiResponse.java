package com.fourthread.ozang.module.domain.weather.dto;

import java.util.List;

public record WeatherApiResponse(
    ResponseData response
) {
    public record ResponseData(
        Header header,
        Body body
    ) {}

    public record Header(
        String resultCode,
        String resultMsg
    ) {}

    public record Body(
        String dataType,
        Items items,
        Integer pageNo,
        Integer numOfRows,
        Integer totalCount
    ) {}

    public record Items(
        List<WeatherItem> item
    ) {}

    public record WeatherItem(
        String baseDate,
        String baseTime,
        String category,
        String fcstDate,
        String fcstTime,
        String fcstValue,
        Integer nx,
        Integer ny
    ) {}
}