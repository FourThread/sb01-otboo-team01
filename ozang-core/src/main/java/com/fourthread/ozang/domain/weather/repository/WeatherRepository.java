package com.fourthread.ozang.domain.weather.repository;

import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WeatherRepository extends JpaRepository<Weather, UUID> {

    //  격자 좌표 기반 조회
    @Query("SELECT w FROM Weather w WHERE " +
        "w.location.x = :x AND w.location.y = :y " +
        "ORDER BY w.forecastedAt DESC "
        + "LIMIT 1")
    Optional<Weather> findLatestByGridCoordinate(
        @Param("x") Integer x,
        @Param("y") Integer y
    );

    @Query("SELECT w FROM Weather w WHERE " +
        "w.location.x = :x AND w.location.y = :y " +
        "AND DATE(w.forecastAt) = :date " +
        "ORDER BY w.forecastedAt DESC " +
        "LIMIT 1")
    Optional<Weather> findLatestByGridCoordinateAndDate(
        @Param("x") Integer x,
        @Param("y") Integer y,
        @Param("date") LocalDateTime date
    );

    /**
     * 오래된 날씨 데이터 개수 조회 (배치용)
     * @param cutoffDate 기준 날짜 (이전 데이터들이 삭제 대상)
     * @return 삭제 대상 데이터 개수
     */
    @Query("SELECT COUNT(w) FROM Weather w WHERE w.forecastedAt < :cutoffDate")
    long countOldWeatherData(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 오래된 날씨 데이터 삭제 (배치용)
     * @param cutoffDate 기준 날짜 (이전 데이터들을 삭제)
     */
    @Modifying
    @Query("DELETE FROM Weather w WHERE w.forecastedAt < :cutoffDate")
    void deleteOldWeatherData(@Param("cutoffDate") LocalDateTime cutoffDate);

    List<Weather> findALlByIdIn(Collection<UUID> ids);
}