package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.data.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.data.WeatherDto;

public interface WeatherService {

    /**
     * 위경도 좌표로 날씨 정보 조회
     */
    WeatherDto getWeatherForecast(Double longitude, Double latitude);

    /**
     * 위경도 좌표로 위치 정보 조회
     */
    WeatherAPILocation getWeatherLocation(Double longitude, Double latitude);
}