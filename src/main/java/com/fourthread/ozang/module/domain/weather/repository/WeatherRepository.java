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
        "ORDER BY w.forecastedAt DESC")
    Optional<Weather> findLatestByGridCoordinate(
        @Param("x") Integer x,
        @Param("y") Integer y
    );
}