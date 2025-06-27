package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import java.util.List;

public interface WeatherService {

    /**
     * 위경도 좌표로 날씨 정보 조회
     */
    WeatherDto getWeatherForecast(Double longitude, Double latitude);

    /**
     * 오늘 실황(초단기실황) + 최대 5일 단기예보
     */
    List<WeatherDto> getFiveDayForecast(Double longitude, Double latitude);

    /**
     * 위경도 좌표로 위치 정보 조회
     */
    WeatherAPILocation getWeatherLocation(Double longitude, Double latitude);
}