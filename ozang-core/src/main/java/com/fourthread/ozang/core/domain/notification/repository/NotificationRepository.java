package com.fourthread.ozang.core.domain.notification.repository;

import com.fourthread.ozang.core.domain.notification.entity.Notification;
import com.fourthread.ozang.core.domain.notification.repository.query.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
}
