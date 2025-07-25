package com.ozang.common.domain.notification.repository.query;

import com.ozang.common.domain.clothes.dto.response.SortDirection;
import com.ozang.common.domain.notification.entity.Notification;

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
