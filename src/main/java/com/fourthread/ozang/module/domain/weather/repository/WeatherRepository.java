package com.fourthread.ozang.module.domain.weather.repository;

import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
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

    /**
     * 특정 날짜 범위의 날씨 데이터 조회 (배치 모니터링용)
     * @param startDate 시작 날짜
     * @param endDate 끝 날짜
     * @return 해당 범위의 날씨 데이터 목록
     */
    @Query("SELECT w FROM Weather w WHERE w.forecastedAt BETWEEN :startDate AND :endDate ORDER BY w.forecastedAt DESC")
    List<Weather> findWeatherDataBetweenDates(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * API 응답 해시로 중복 체크
     */
    boolean existsByApiResponseHash(String apiResponseHash);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 특정 예보 시간의 날씨 데이터 조회
     */
    @Query("SELECT w FROM Weather w WHERE w.forecastAt BETWEEN :startTime AND :endTime")
    List<Weather> findByForecastTimeBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 지역별 최신 날씨 데이터 조회
     */
    @Query("SELECT w FROM Weather w WHERE " +
        "w.location.locationNames LIKE %:location% " +
        "AND w.forecastedAt = (SELECT MAX(w2.forecastedAt) FROM Weather w2 " +
        "WHERE w2.location.locationNames LIKE %:location%)")
    Optional<Weather> findLatestByLocation(@Param("location") String location);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 특정 온도 범위의 날씨 데이터 조회
     */
    @Query("SELECT w FROM Weather w WHERE " +
        "w.temperature.current BETWEEN :minTemp AND :maxTemp " +
        "AND w.forecastAt BETWEEN :startDate AND :endDate")
    List<Weather> findByTemperatureRangeAndDate(
        @Param("minTemp") Double minTemp,
        @Param("maxTemp") Double maxTemp,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 강수 타입별 날씨 데이터 조회
     */
    @Query("SELECT w FROM Weather w WHERE " +
        "w.precipitation.type = :precipitationType " +
        "AND w.forecastAt BETWEEN :startDate AND :endDate")
    List<Weather> findByPrecipitationTypeAndDate(
        @Param("precipitationType") String precipitationType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 날씨 상태별 통계 조회 (배치 분석용)
     */
    @Query("SELECT w.skyStatus, COUNT(w) FROM Weather w " +
        "WHERE w.forecastAt BETWEEN :startDate AND :endDate " +
        "GROUP BY w.skyStatus")
    List<Object[]> countBySkyStatusAndDate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 지역별 평균 온도 조회 (배치 통계용)
     */
    @Query("SELECT w.location.locationNames, AVG(w.temperature.current) " +
        "FROM Weather w " +
        "WHERE w.forecastAt BETWEEN :startDate AND :endDate " +
        "GROUP BY w.location.locationNames")
    List<Object[]> averageTemperatureByLocationAndDate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 극한 날씨 조건 조회 (알림 배치용)
     */
    @Query("SELECT w FROM Weather w WHERE " +
        "(w.temperature.max >= :heatThreshold OR " +
        "w.temperature.min <= :coldThreshold OR " +
        "w.precipitation.amount >= :rainThreshold OR " +
        "w.wind.speed >= :windThreshold) " +
        "AND w.forecastAt BETWEEN :startDate AND :endDate")
    List<Weather> findExtremeWeatherConditions(
        @Param("heatThreshold") Double heatThreshold,
        @Param("coldThreshold") Double coldThreshold,
        @Param("rainThreshold") Double rainThreshold,
        @Param("windThreshold") Double windThreshold,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}