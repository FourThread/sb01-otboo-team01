package com.ozang.common.domain.notification.repository;

import com.ozang.common.domain.notification.entity.Notification;
import com.ozang.common.domain.notification.repository.query.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
}
