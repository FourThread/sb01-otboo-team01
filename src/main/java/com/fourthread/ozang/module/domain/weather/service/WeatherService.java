package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    /**
     * =============== 새로운 메서드 추가 ===============
     * 특정 위치의 날씨 데이터 동기화 (배치용)
     * @param longitude 경도
     * @param latitude 위도
     * @return 동기화된 날씨 데이터
     */
    List<Weather> syncWeatherData(Double longitude, Double latitude);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 여러 위치의 날씨 데이터 일괄 동기화 (배치용)
     * @param locations 위치 목록 (경도, 위도 쌍)
     * @return 위치별 동기화 결과
     */
    Map<String, List<Weather>> syncMultipleLocations(List<Map<String, Double>> locations);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 날씨 통계 데이터 조회 (배치 분석용)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 통계 데이터
     */
    WeatherStatistics getWeatherStatistics(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 극한 날씨 조건 감지 (알림 배치용)
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 극한 날씨 목록
     */
    List<ExtremeWeatherInfo> detectExtremeWeatherConditions(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 날씨 기반 추천 데이터 생성 (추천 배치용)
     * @param weatherCondition 날씨 조건
     * @return 추천 데이터
     */
    WeatherRecommendationData generateRecommendationData(WeatherCondition weatherCondition);

    /**
     * =============== 내부 클래스 정의 ===============
     */
    class WeatherStatistics {
        private Map<String, Double> averageTemperatureByLocation;
        private Map<String, Integer> precipitationDaysByLocation;
        private Map<String, Integer> clearDaysByLocation;
        private double overallAverageTemperature;
        private int totalDataPoints;

        // Getters and Setters
        public Map<String, Double> getAverageTemperatureByLocation() { return averageTemperatureByLocation; }
        public void setAverageTemperatureByLocation(Map<String, Double> averageTemperatureByLocation) {
            this.averageTemperatureByLocation = averageTemperatureByLocation;
        }

        public Map<String, Integer> getPrecipitationDaysByLocation() { return precipitationDaysByLocation; }
        public void setPrecipitationDaysByLocation(Map<String, Integer> precipitationDaysByLocation) {
            this.precipitationDaysByLocation = precipitationDaysByLocation;
        }

        public Map<String, Integer> getClearDaysByLocation() { return clearDaysByLocation; }
        public void setClearDaysByLocation(Map<String, Integer> clearDaysByLocation) {
            this.clearDaysByLocation = clearDaysByLocation;
        }

        public double getOverallAverageTemperature() { return overallAverageTemperature; }
        public void setOverallAverageTemperature(double overallAverageTemperature) {
            this.overallAverageTemperature = overallAverageTemperature;
        }

        public int getTotalDataPoints() { return totalDataPoints; }
        public void setTotalDataPoints(int totalDataPoints) { this.totalDataPoints = totalDataPoints; }
    }

    class ExtremeWeatherInfo {
        private Weather weather;
        private String alertType;
        private String severity;
        private String location;
        private LocalDateTime alertTime;

        // Getters and Setters
        public Weather getWeather() { return weather; }
        public void setWeather(Weather weather) { this.weather = weather; }

        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public LocalDateTime getAlertTime() { return alertTime; }
        public void setAlertTime(LocalDateTime alertTime) { this.alertTime = alertTime; }
    }

    class WeatherCondition {
        private String temperatureRange;
        private String skyStatus;
        private String season;
        private boolean precipitation;

        // Getters and Setters
        public String getTemperatureRange() { return temperatureRange; }
        public void setTemperatureRange(String temperatureRange) { this.temperatureRange = temperatureRange; }

        public String getSkyStatus() { return skyStatus; }
        public void setSkyStatus(String skyStatus) { this.skyStatus = skyStatus; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }

        public boolean isPrecipitation() { return precipitation; }
        public void setPrecipitation(boolean precipitation) { this.precipitation = precipitation; }
    }

    class WeatherRecommendationData {
        private WeatherCondition condition;
        private List<String> recommendedClothesTypes;
        private Map<String, Double> confidenceScores;
        private String recommendationMessage;
        private LocalDateTime generatedAt;

        // Getters and Setters
        public WeatherCondition getCondition() { return condition; }
        public void setCondition(WeatherCondition condition) { this.condition = condition; }

        public List<String> getRecommendedClothesTypes() { return recommendedClothesTypes; }
        public void setRecommendedClothesTypes(List<String> recommendedClothesTypes) {
            this.recommendedClothesTypes = recommendedClothesTypes;
        }

        public Map<String, Double> getConfidenceScores() { return confidenceScores; }
        public void setConfidenceScores(Map<String, Double> confidenceScores) {
            this.confidenceScores = confidenceScores;
        }

        public String getRecommendationMessage() { return recommendationMessage; }
        public void setRecommendationMessage(String recommendationMessage) {
            this.recommendationMessage = recommendationMessage;
        }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }
}