package com.fourthread.ozang.module.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record WeatherApiResponse(
    @JsonProperty("response")
    ResponseData response,

    // =============== XML 직접 매핑용 (XML일 때 사용) ===============
    @JsonProperty("header")
    Header header,

    @JsonProperty("body")
    Body body
) {
    // =============== 안전한 접근 메소드들 ===============

    /**
     * 응답 구조에 관계없이 안전하게 header에 접근
     */
    public Header getHeader() {
        if (response != null && response.header() != null) {
            return response.header();
        }
        return header;
    }

    /**
     * 응답 구조에 관계없이 안전하게 body에 접근
     */
    public Body getBody() {
        if (response != null && response.body() != null) {
            return response.body();
        }
        return body;
    }

    /**
     * 안전하게 items에 접근
     */
    public List<WeatherItem> getItems() {
        Body bodyData = getBody();
        if (bodyData != null && bodyData.items() != null) {
            return bodyData.items().item();
        }
        return List.of();
    }

    /**
     * 결과 코드 확인
     */
    public String getResultCode() {
        Header headerData = getHeader();
        return headerData != null ? headerData.resultCode() : null;
    }

    public record ResponseData(
        @JsonProperty("header")
        Header header,

        @JsonProperty("body")
        Body body
    ) {}

    public record Header(
        @JsonProperty("resultCode")
        String resultCode,

        @JsonProperty("resultMsg")
        String resultMsg
    ) {}

    public record Body(
        @JsonProperty("dataType")
        String dataType,

        @JsonProperty("items")
        Items items,

        @JsonProperty("pageNo")
        Integer pageNo,

        @JsonProperty("numOfRows")
        Integer numOfRows,

        @JsonProperty("totalCount")
        Integer totalCount
    ) {}

    public record Items(
        @JsonProperty("item")
        List<WeatherItem> item
    ) {}

    public record WeatherItem(
        @JsonProperty("baseDate")
        String baseDate,

        @JsonProperty("baseTime")
        String baseTime,

        @JsonProperty("category")
        String category,

        @JsonProperty("fcstDate")
        String fcstDate,

        @JsonProperty("fcstTime")
        String fcstTime,

        @JsonProperty("fcstValue")
        String fcstValue,

        @JsonProperty("nx")
        Integer nx,

        @JsonProperty("ny")
        Integer ny
    ) {}
}
//public record WeatherApiResponse(
//    ResponseData response
//) {
//    public record ResponseData(
//        Header header,
//        Body body
//    ) {}
//
//    public record Header(
//        String resultCode,
//        String resultMsg
//    ) {}
//
//    public record Body(
//        String dataType,
//        Items items,
//        Integer pageNo,
//        Integer numOfRows,
//        Integer totalCount
//    ) {}
//
//    public record Items(
//        List<WeatherItem> item
//    ) {}
//
//    public record WeatherItem(
//        String baseDate,
//        String baseTime,
//        String category,
//        String fcstDate,
//        String fcstTime,
//        String fcstValue,
//        Integer nx,
//        Integer ny
//    ) {}
//}