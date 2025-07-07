package com.fourthread.ozang.module.domain.notification.service;

import com.fourthread.ozang.module.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.module.domain.notification.dto.response.NotificationCursorResponse;
import com.fourthread.ozang.module.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.mapper.NotificationMapper;
import com.fourthread.ozang.module.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public NotificationCursorResponse findAllByReceiver(UUID receiverId, String cursor, UUID idAfter, int limit) {
        SortDirection direction = SortDirection.DESCENDING; // 현재는 고정된 정렬 방향 (DESC)

        // limit + 1로 초과 조회 → hasNext 판단용
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

    @Transactional
    public void delete(UUID receiverId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림이 존재하지 않습니다"));

        if (!notification.getReceiverId().equals(receiverId)) {
            throw new AccessDeniedException("삭제 권한이 없습니다.");
        }

        notificationRepository.delete(notification);
    }
}