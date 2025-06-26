package com.fourthread.ozang.module.domain.weather.controller;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weathers")
@Slf4j
@RequiredArgsConstructor
@Validated
@Tag(name = "날씨 관리", description = "날씨 관련 API")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<WeatherDto> getWeather(
        @Parameter(description = "경도", required = true, example = "126.9780")
        @RequestParam
        @NotNull(message = "경도는 필수입니다")
        @Min(value = 124, message = "경도는 124 이상이어야 합니다")
        @Max(value = 132, message = "경도는 132 이하여야 합니다")
        Double longitude,

        @Parameter(description = "위도", required = true, example = "37.5665")
        @RequestParam
        @NotNull(message = "위도는 필수입니다")
        @Min(value = 33, message = "위도는 33 이상이어야 합니다")
        @Max(value = 43, message = "위도는 43 이하여야 합니다")
        Double latitude
    ) {
        log.info("날씨 정보 요청 - 위도: {}, 경도: {}", latitude, longitude);

        WeatherDto weatherData = weatherService.getWeatherForecast(longitude, latitude);

        return ResponseEntity.ok(weatherData);
    }

    @GetMapping("/location")
    public ResponseEntity<WeatherAPILocation> getWeatherLocation(
        @Parameter(description = "경도", required = true, example = "126.9780")
        @RequestParam
        @NotNull(message = "경도는 필수입니다")
        @Min(value = 124, message = "경도는 124 이상이어야 합니다")
        @Max(value = 132, message = "경도는 132 이하여야 합니다")
        Double longitude,

        @Parameter(description = "위도", required = true, example = "37.5665")
        @RequestParam
        @NotNull(message = "위도는 필수입니다")
        @Min(value = 33, message = "위도는 33 이상이어야 합니다")
        @Max(value = 43, message = "위도는 43 이하여야 합니다")
        Double latitude
    ) {
        log.info("위치 정보 요청 - 위도: {}, 경도: {}", latitude, longitude);

        WeatherAPILocation location = weatherService.getWeatherLocation(longitude, latitude);

        return ResponseEntity.ok(location);
    }
}

record ErrorResponse(
    @Schema(description = "예외 이름", example = "InvalidCoordinateException")
    String exceptionName,

    @Schema(description = "오류 메시지", example = "잘못된 좌표입니다.")
    String message,

    @Schema(description = "오류 부가 정보")
    java.util.Map<String, String> details
) {}