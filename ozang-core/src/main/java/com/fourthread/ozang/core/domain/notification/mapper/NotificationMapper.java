package com.fourthread.ozang.core.domain.notification.mapper;

import com.fourthread.ozang.core.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.core.domain.notification.entity.Notification;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification notification) {
        Instant createdAtInstant = notification.getCreatedAt() != null
            ? notification.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
            : Instant.now();

        return new NotificationDto(
            notification.getId(),
            createdAtInstant,
            notification.getReceiverId(),
            notification.getTitle(),
            notification.getContent(),
            notification.getLevel()
        );
    }

    public List<NotificationDto> toDtoList(List<Notification> notifications) {
        return notifications.stream()
                .map(this::toDto)
                .toList();
    }

}
