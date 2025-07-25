package com.fourthread.ozang.domain.notification.service;

import static com.fourthread.ozang.common.exception.ErrorCode.*;

import com.fourthread.ozang.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.domain.notification.dto.response.NotificationCursorResponse;
import com.fourthread.ozang.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.domain.notification.entity.Notification;
import com.fourthread.ozang.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.domain.notification.event.MultipleNotificationCreatedEvent;
import com.fourthread.ozang.domain.notification.event.NotificationCreatedEvent;
import com.fourthread.ozang.domain.notification.execption.NotificationException;
import com.fourthread.ozang.domain.notification.mapper.NotificationMapper;
import com.fourthread.ozang.domain.notification.repository.NotificationRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
        log.debug("알림 목록 조회 시작: receiverId={}, cursor={}, idAfter={}, limit={}", receiverId, cursor, idAfter, limit);
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

        log.info("알림 목록 조회 완료: count={}, hasNext={}", dtoList.size(), hasNext);
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
        log.debug("알림 삭제 시작: receiverId={}, notificationId={}", receiverId, notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("알림 삭제 실패 - 존재하지 않음: notificationId={}", notificationId);
                    return new NotificationException(NOTIFICATION_NOT_FOUND, this.getClass().getSimpleName(), NOTIFICATION_NOT_FOUND.getMessage());
                });

        if (!notification.getReceiverId().equals(receiverId)) {
            log.warn("알림 삭제 실패 - 권한 없음: receiverId={}, notificationId={}", receiverId, notificationId);
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        notificationRepository.delete(notification);
        log.info("알림 삭제 완료: notificationId={}", notificationId);
    }


    @Transactional
    public void create(UUID receiverId, String title, String content,
                       NotificationLevel level) {
        log.debug("단일 알림 생성 시작: receiverId={}, level={}, title={}", receiverId, level, title);
        Notification notification = new Notification(
                receiverId,
                title,
                content,
                level
        );

        notificationRepository.save(notification);

        NotificationDto dto = notificationMapper.toDto(notification);
        eventPublisher.publishEvent(new NotificationCreatedEvent(dto));

        log.info("단일 알림 생성 완료: id={}, receiverId={}", notification.getId(), receiverId);
    }

    @Transactional
    public void createAll(Set<UUID> receiverIds, String title, String content,
                          NotificationLevel level) {
        log.debug("여러 알림 생성 시작: count={}, level={}, title={}", receiverIds.size(), level, title);

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

        log.info("여러 알림 생성 완료: count={}", dtos.size());
    }
}
