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

    /**
     * 오래된 날씨 데이터 정리 (배치용)
     * @return 삭제된 데이터 개수
     */
    int cleanupOldWeatherData();

    /**
     * 오래된 날씨 데이터 정리 (배치용 - 사용자 정의 보관 기간)
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 데이터 개수
     */
    int cleanupOldWeatherData(int retentionDays);
}