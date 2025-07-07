package com.fourthread.ozang.module.domain.notification.mapper;

import com.fourthread.ozang.module.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.module.domain.notification.entity.Notification;

import java.time.ZoneId;

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

}
