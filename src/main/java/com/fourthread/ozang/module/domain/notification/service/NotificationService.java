package com.fourthread.ozang.module.domain.notification.service;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.notification.dto.response.NotificationCursorResponse;
import com.fourthread.ozang.module.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.event.MultipleNotificationCreatedEvent;
import com.fourthread.ozang.module.domain.notification.event.NotificationCreatedEvent;
import com.fourthread.ozang.module.domain.notification.mapper.NotificationMapper;
import com.fourthread.ozang.module.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NotificationCursorResponse findAllByReceiver(UUID receiverId, String cursor, UUID idAfter, int limit) {
        SortDirection direction = SortDirection.DESCENDING; // 현재는 고정된 정렬 방향 (DESC)

        List<Notification> results = notificationRepository.findAllByCondition(
                receiverId,
                cursor,
                idAfter,
                limit + 1,
                direction
        );

        boolean hasNext = results.size() > limit;
        List<Notification> pageContent = hasNext ? results.subList(0, limit) : results;

        UUID nextId = hasNext ? pageContent.get(pageContent.size() - 1).getId() : null;
        String nextCursor = hasNext ? getCursorValue(pageContent.get(pageContent.size() - 1)) : null;

        int totalCount = notificationRepository.countByReceiverId(receiverId);
        List<NotificationDto> dtoList = notificationMapper.toDtoList(pageContent);

        return new NotificationCursorResponse(
                dtoList,
                nextCursor,
                nextId,
                hasNext,
                totalCount,
                "createdAt",
                direction.name()
        );
    }

    private String getCursorValue(Notification notification) {
        return notification.getCreatedAt().toString(); // LocalDateTime → String (ISO8601)
    }

    @Transactional
    public void delete(UUID receiverId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림이 존재하지 않습니다")); //TODO 커스텀 예외처리

        if (!notification.getReceiverId().equals(receiverId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다."); //TODO 다른 방식으로 권한 처리 고민해보기
        }

        notificationRepository.delete(notification);
    }



    @Transactional
    public void create(UUID receiverId, String title, String content,
                       NotificationLevel level, UUID targetId) {
        log.debug("새 알림 생성 시작: receiverId={}, type={}, targetId={}", receiverId, level, targetId);

        Notification notification = new Notification(
                receiverId,
                title,
                content,
                level
        );

        notificationRepository.save(notification);

        NotificationDto dto = notificationMapper.toDto(notification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(dto));

        log.info("새 알림 생성 완료: id={}, receiverId={}, targetId={}",
                notification.getId(), receiverId, targetId);
    }

    @Transactional
    public void createAll(Set<UUID> receiverIds, String title, String content,
                          NotificationLevel level, UUID targetId) {
        log.debug("여러 알림 생성 시작: receiverIds={}, type={}, targetId={}", receiverIds, level, targetId);

        List<Notification> notifications = receiverIds.stream()
                .map(receiverId -> new Notification(
                        receiverId,
                        title,
                        content,
                        level
                )).toList();

        notificationRepository.saveAll(notifications);

        List<NotificationDto> dtos = notificationMapper.toDtoList(notifications);
        eventPublisher.publishEvent(new MultipleNotificationCreatedEvent(dtos));

        log.info("여러 알림 생성 완료: count={}, targetId={}", dtos.size(), targetId);
    }
}
