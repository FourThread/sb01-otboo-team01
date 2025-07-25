package com.fourthread.ozang.domain.notification.repository;

import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.repository.query.NotificationRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>, NotificationRepositoryCustom {
}
