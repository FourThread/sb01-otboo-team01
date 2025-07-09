package com.fourthread.ozang.module.domain.notification.repository;

import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.repository.query.NotificationRepositoryCustom;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {

    /**
        * =============== 새로운 메서드 추가 ===============
        * 날짜별 알림 통계 조회
     */
    @Query("SELECT n.level, COUNT(n) FROM Notification n " +
        "WHERE n.createdAt BETWEEN :startDate AND :endDate " +
        "GROUP BY n.level")
    List<Object[]> countByLevelAndDate(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * =============== 새로운 메서드 추가 ===============
     * 사용자별 최근 날씨 알림 조회
     */
    @Query("SELECT n FROM Notification n " +
        "WHERE n.receiver.id = :userId " +
        "AND n.title LIKE '%날씨%' " +
        "ORDER BY n.createdAt DESC")
    List<Notification> findRecentWeatherNotificationsByUser(@Param("userId") UUID userId);
}
