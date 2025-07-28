package com.fourthread.ozang.core.domain.weather.service;

import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherChangeDto;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import java.util.List;

public interface WeatherService {

    /**
     * 위경도 좌표로 초단기예보 정보 조회 (6시간 이내, 1시간 단위)
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
     * 날씨 변화 감지 (배치용)
     * @param latitude 위도
     * @param longitude 경도
     * @return 감지된 변화 목록
     */
    List<WeatherChangeDto> detectWeatherChanges(Double latitude, Double longitude);

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