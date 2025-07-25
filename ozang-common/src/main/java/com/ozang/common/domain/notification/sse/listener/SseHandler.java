package com.ozang.common.domain.notification.sse.listener;

import com.ozang.common.domain.notification.dto.response.NotificationDto;
import com.ozang.common.domain.notification.event.MultipleNotificationCreatedEvent;
import com.ozang.common.domain.notification.event.NotificationCreatedEvent;
import com.ozang.common.domain.notification.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class SseHandler {

    private final SseService sseService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationCreatedEvent event) {
        NotificationDto notification = event.notificationDto();
        sseService.send(notification.receiverId(), "notifications", notification);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MultipleNotificationCreatedEvent event) {
        for (NotificationDto notification : event.notifications()) {
            sseService.send(notification.receiverId(), "notifications", notification);
        }
    }
}