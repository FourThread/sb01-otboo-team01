package com.ozang.web.notification.repository.query;

import com.ozang.web.clothes.dto.response.SortDirection;
import com.ozang.web.notification.entity.Notification;

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
