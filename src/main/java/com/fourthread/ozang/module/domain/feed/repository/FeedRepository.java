package com.fourthread.ozang.module.domain.feed.repository;

import com.fourthread.ozang.module.domain.feed.entity.Feed;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {
    /**
     * =============== 새로운 메서드 추가 ===============
     * 특정 날짜 이후의 피드 조회 (배치 분석용)
     */
    List<Feed> findByCreatedAtAfter(LocalDateTime date);

    /**
     * =============== 새로운 메서드 추가 ===============
     * 날씨 조건별 피드 조회 (추천 분석용)
     */
    @Query("SELECT f FROM Feed f " +
        "JOIN f.weather w " +
        "WHERE w.temperature.current BETWEEN :minTemp AND :maxTemp " +
        "AND f.createdAt BETWEEN :startDate AND :endDate")
    List<Feed> findByWeatherTemperatureRange(
        @Param("minTemp") Double minTemp,
        @Param("maxTemp") Double maxTemp,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 인기 피드 조회 (좋아요 수 기준)
     */
    @Query("SELECT f FROM Feed f " +
        "WHERE f.createdAt BETWEEN :startDate AND :endDate " +
        "ORDER BY f.likeCount DESC")
    List<Feed> findPopularFeedsByDate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
