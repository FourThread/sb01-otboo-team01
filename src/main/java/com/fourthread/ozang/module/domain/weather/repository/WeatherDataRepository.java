package com.fourthread.ozang.module.domain.weather.repository;

import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherDataRepository extends JpaRepository<Weather, UUID> {

    @Query("SELECT w FROM Weather w WHERE w.x = :x AND w.y = :y AND w.forecastAt = :forecastAt")
    Optional<Weather> findByCoordinateAndForecastAt(@Param("x") Integer x, @Param("y") Integer y, @Param("forecastAt") LocalDateTime forecastAt);

    @Query("SELECT w FROM Weather w WHERE w.x = :x AND w.y = :y AND w.forecastAt >= :startTime AND w.forecastAt <= :endTime ORDER BY w.forecastAt")
    List<Weather> findByCoordinateAndTimeRange(@Param("x") Integer x, @Param("y") Integer y, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    // =============== 문제가 될 수 있는 쿼리 - 정확한 좌표 매칭 필요 ===============
    @Query("SELECT w FROM Weather w WHERE w.latitude = :latitude AND w.longitude = :longitude ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestByLocation(@Param("latitude") Double latitude, @Param("longitude") Double longitude);

    // =============== 더 정확한 좌표 매칭을 위한 새 쿼리 추가 ===============
    @Query("SELECT w FROM Weather w WHERE " +
        "ABS(w.latitude - :latitude) < 0.01 AND " +
        "ABS(w.longitude - :longitude) < 0.01 " +
        "ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestByLocationWithTolerance(@Param("latitude") Double latitude, @Param("longitude") Double longitude);

    // =============== 격자 좌표로 최신 데이터 조회 (더 정확함) ===============
    @Query("SELECT w FROM Weather w WHERE w.x = :x AND w.y = :y ORDER BY w.forecastedAt DESC LIMIT 1")
    Optional<Weather> findLatestByGridCoordinate(@Param("x") Integer x, @Param("y") Integer y);

    @Query("SELECT DISTINCT NEW com.fourthread.ozang.module.domain.weather.entity.GridCoordinate(w.x, w.y) FROM Weather w")
    List<GridCoordinate> findDistinctCoordinates();

    boolean existsByApiResponseHash(String hash);

    void deleteByForecastAtBefore(LocalDateTime dateTime);
}