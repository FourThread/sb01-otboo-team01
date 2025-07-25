package com.fourthread.ozang.app.domain.notification.service;

import com.fourthread.ozang.app.domain.clothes.dto.response.SortDirection;
import com.fourthread.ozang.app.domain.follow.repository.FollowRepository;
import com.fourthread.ozang.app.domain.notification.dto.response.NotificationCursorResponse;
import com.fourthread.ozang.app.domain.notification.dto.response.NotificationDto;
import com.fourthread.ozang.app.domain.notification.entity.Notification;
import com.fourthread.ozang.app.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.event.*;
import com.fourthread.ozang.app.domain.notification.execption.NotificationException;
import com.fourthread.ozang.app.domain.notification.mapper.NotificationMapper;
import com.fourthread.ozang.app.domain.notification.repository.NotificationRepository;
import com.fourthread.ozang.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.fourthread.ozang.app.common.exception.ErrorCode.*;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

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


    @Transactional
    public void sendRoleChangedNotification(RoleChangedEvent event) {
        UUID receiverId = event.userDto().id();
        String title = "권한이 변경되었어요.";
        String content = String.format("관리자가 당신의 권한을 [%s]으로 변경하였습니다.", event.userDto().role().name());

        create(receiverId, title, content, NotificationLevel.INFO);
    }

    @Transactional
    public void sendClothesAttributeAddedNotification(ClothesAttributeAddedEvent event) {
        Set<UUID> allUserIds = userRepository.findAllUserIds();
        Set<UUID> otherUserIds = allUserIds.stream()
                .filter(id -> !id.equals(event.requesterId()))
                .collect(Collectors.toSet());

        String title = "새로운 의상 속성이 추가되었어요.";
        String content = String.format("[%s] 의상에 속성이 추가되었습니다.", event.clothesAttributeDefDto().name());

        createAll(otherUserIds, title, content, NotificationLevel.INFO);
    }

    @Transactional
    public void sendClothesAttributeUpdatedNotification(ClothesAttributeUpdatedEvent event) {
        Set<UUID> allUserIds = userRepository.findAllUserIds();
        Set<UUID> otherUserIds = allUserIds.stream()
                .filter(id -> !id.equals(event.requesterId()))
                .collect(Collectors.toSet());

        String title = "의상 속성이 변경되었어요.";
        String content = String.format("[%s] 속성을 확인해보세요.", event.clothesAttributeDefDto().name());

        createAll(otherUserIds, title, content, NotificationLevel.INFO);
    }

    @Transactional
    public void sendFeedLikedNotification(FeedLikedEvent event) {
        String title = String.format("%s님이 내 피드를 좋아합니다.", event.likeByUserName());
        create(event.feedUserId(), title, event.content(), NotificationLevel.INFO);
    }

    @Transactional
    public void sendFeedCommentedNotification(FeedCommentedEvent event) {
        String title = String.format("%s님이 댓글을 달았어요.", event.commentUserName());
        create(event.feedAuthorUserId(), title, event.content(), NotificationLevel.INFO);
    }

    @Transactional
    public void sendFollowingFeedCreatedNotification(FollowingFeedCreatedEvent event) {
        UUID followeeId = event.userSummary().userId();
        Set<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(followeeId);

        String title = String.format("%s님이 새로운 피드를 작성했어요.", event.userSummary().name());
        createAll(followerIds, title, event.content(), NotificationLevel.INFO);
    }

    @Transactional
    public void sendFollowedNotification(FollowedEvent event) {
        UUID receiverId = event.dto().followee().userId();
        String followerName = event.dto().follower().name();

        String title = String.format("%s님이 나를 팔로우했어요.", followerName);
        create(receiverId, title, "", NotificationLevel.INFO);
    }

    @Transactional
    public void sendDmReceivedNotification(DmReceivedEvent event) {
        UUID receiverId = event.dmDto().receiver().userId();
        String senderName = event.dmDto().sender().name();

        String title = String.format("[DM] %s", senderName);
        create(receiverId, title, event.dmDto().content(), NotificationLevel.INFO);
    }


    @Transactional
    public void sendWeatherAlertNotification(WeatherChangeDetectedEvent event) {
        Set<UUID> receiverIds = userRepository.findAllUserIds();

        createAll(receiverIds,
                event.title(),
                event.content(),
                event.level()
                );
    }


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
}
