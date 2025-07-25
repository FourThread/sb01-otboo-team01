package com.fourthread.ozang.domain.notification.mapper;

import com.fourthread.ozang.module.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.module.domain.notification.entity.Notification;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Component;

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
