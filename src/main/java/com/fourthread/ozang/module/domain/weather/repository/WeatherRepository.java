package com.fourthread.ozang.module.domain.weather.repository;

import com.fourthread.ozang.module.domain.weather.entity.Weather;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    /**
     * 특정 위치와 시간대의 최신 날씨 정보 조회
     */
    @Query("SELECT w FROM Weather w WHERE w.latitude = :latitude AND w.longitude = :longitude " +
        "AND w.forecastAt >= :startTime AND w.forecastAt <= :endTime " +
        "ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestWeatherByLocationAndTime(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 위치의 최신 날씨 정보 조회
     */
    @Query("SELECT w FROM Weather w WHERE w.latitude = :latitude AND w.longitude = :longitude " +
        "ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestWeatherByLocation(
        @Param("latitude") Double latitude,
        @Param("longitude") Double longitude
    );

    /**
     * x, y 좌표로 최신 날씨 정보 조회
     */
    @Query("SELECT w FROM Weather w WHERE w.x = :x AND w.y = :y " +
        "ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestWeatherByXY(
        @Param("x") Integer x,
        @Param("y") Integer y
    );

    /**
     * 오래된 날씨 데이터 삭제 (데이터 정리용)
     */
    @Query("DELETE FROM Weather w WHERE w.forecastedAt < :cutoffDate")
    void deleteOldWeatherData(@Param("cutoffDate") LocalDateTime cutoffDate);
}