package com.fourthread.ozang.module.domain.notification.repository.query;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {
    List<Notification> findAllByCondition(UUID receiverId,
                                          String cursor,
                                          UUID idAfter,
                                          int limit,
                                          SortDirection direction);

    int countByReceiverId(UUID receiverId);
}
