package com.ozang.web.notification.repository;

import com.ozang.web.notification.entity.Notification;
import com.ozang.web.notification.repository.query.NotificationRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
}
