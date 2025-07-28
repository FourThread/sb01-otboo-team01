package com.fourthread.ozang.core.domain.notification.repository.query;

import com.fourthread.ozang.core.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.core.domain.notification.entity.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {
    List<Notification> findAllByCondition(UUID receiverId,
                                          String cursor,
                                          UUID idAfter,
                                          int limit,
                                          com.fourthread.ozang.core.domain.clothes.dto.response.SortDirection direction);

    int countByReceiverId(UUID receiverId);
}
