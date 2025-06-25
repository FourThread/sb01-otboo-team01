package com.fourthread.ozang.module.domain.weather.controller;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/weathers")
@RequiredArgsConstructor
@Tag(name = "날씨 관리", description = "날씨 관련 API")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    @Operation(summary = "날씨 정보 조회", description = "날씨 정보 조회 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "날씨 조회 성공"),
        @ApiResponse(responseCode = "400", description = "날씨 조회 실패")
    })
    public ResponseEntity<WeatherDto> getWeather(
        @Parameter(description = "경도", required = true)
        @RequestParam Double longitude,
        @Parameter(description = "위도", required = true)
        @RequestParam Double latitude) {

        log.debug("Getting weather for longitude: {}, latitude: {}", longitude, latitude);

        WeatherDto weather = weatherService.getWeatherForecast(longitude, latitude);
        return ResponseEntity.ok(weather);
    }

    @GetMapping("/location")
    @Operation(summary = "날씨 위치 정보 조회", description = "날씨 위치 정보 조회 API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "날씨 위치 정보 조회 성공"),
        @ApiResponse(responseCode = "400", description = "날씨 위치 정보 조회 실패")
    })
    public ResponseEntity<WeatherAPILocation> getWeatherLocation(
        @Parameter(description = "경도", required = true)
        @RequestParam Double longitude,
        @Parameter(description = "위도", required = true)
        @RequestParam Double latitude) {

        log.debug("Getting weather location for longitude: {}, latitude: {}", longitude, latitude);

        WeatherAPILocation location = weatherService.getWeatherLocation(longitude, latitude);
        return ResponseEntity.ok(location);
    }
}