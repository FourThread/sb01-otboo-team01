package com.fourthread.ozang.module.domain.weather.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 기상청 API 좌표 변환 관련 응답 DTO들
 * 모든 관련 클래스들을 하나의 파일에 정의하여 응집도를 높임
 */

/**
 * 기상청 API 좌표 변환 최상위 응답
 */
record CoordinateResponse(
    @JsonProperty("response") CoordinateResponseBody response
) {}

/**
 * 기상청 API 좌표 변환 응답 본문
 */
record CoordinateResponseBody(
    @JsonProperty("header") WeatherApiHeader header,
    @JsonProperty("body") CoordinateBody body
) {}

/**
 * 기상청 API 좌표 변환 본문
 */
record CoordinateBody(
    @JsonProperty("items") CoordinateItems items
) {}

/**
 * 기상청 API 좌표 변환 아이템들
 */
record CoordinateItems(
    @JsonProperty("item") List<CoordinateItem> item
) {}

/**
 * 기상청 API 좌표 변환 개별 아이템
 */
record CoordinateItem(
    @JsonProperty("regId") String regId,
    @JsonProperty("step1") String step1,
    @JsonProperty("step2") String step2,
    @JsonProperty("step3") String step3,
    @JsonProperty("gridX") Integer gridX,
    @JsonProperty("gridY") Integer gridY,
    @JsonProperty("longitudeHour") Double longitudeHour,
    @JsonProperty("longitudeMin") Double longitudeMin,
    @JsonProperty("longitudeSec") Double longitudeSec,
    @JsonProperty("latitudeHour") Double latitudeHour,
    @JsonProperty("latitudeMin") Double latitudeMin,
    @JsonProperty("latitudeSec") Double latitudeSec
) {}