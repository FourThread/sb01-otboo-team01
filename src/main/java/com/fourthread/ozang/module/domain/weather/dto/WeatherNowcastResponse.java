package com.fourthread.ozang.module.domain.weather.dto;

import java.util.List;

public record WeatherNowcastResponse(
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
        List<WeatherNowcastItem> item
    ) {}

    public record WeatherNowcastItem(
        String baseDate,
        String baseTime,
        String category,
        String obsrValue,
        Integer nx,
        Integer ny
    ) {}
}
