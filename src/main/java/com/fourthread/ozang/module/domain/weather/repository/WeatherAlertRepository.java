package com.fourthread.ozang.module.domain.weather.repository;

import com.fourthread.ozang.module.domain.weather.entity.WeatherAlert;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, UUID> {

    @Query("SELECT wa FROM WeatherAlert wa WHERE wa.processed = false ORDER BY wa.createdAt DESC")
    List<WeatherAlert> findUnprocessedAlerts();

    @Query("SELECT wa FROM WeatherAlert wa WHERE wa.gridCoordinate.x = :x AND wa.gridCoordinate.y = :y " +
        "AND wa.createdAt >= :since ORDER BY wa.createdAt DESC")
    List<WeatherAlert> findByCoordinateAndCreatedAtAfter(
        @Param("x") Integer x,
        @Param("y") Integer y,
        @Param("since") LocalDateTime since
    );

    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
