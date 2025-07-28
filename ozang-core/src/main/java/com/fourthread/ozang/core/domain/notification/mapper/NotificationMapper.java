package com.fourthread.ozang.core.domain.notification.mapper;

import com.fourthread.ozang.core.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.core.domain.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.List;

@Component
public class NotificationMapper {

    public NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
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
