package com.fourthread.ozang.module.domain.notification.listener;

import com.fourthread.ozang.module.domain.notification.entity.Notification;
import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.event.*;
import com.fourthread.ozang.module.domain.notification.service.NotificationService;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // 권한 변경 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleChangedEvent event) {

    }

    // 의상 속성 추가 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeAddedEvent event) {

        Set<UUID> allUserIds = userRepository.findAllUserIds();

        Set<UUID> otherUserIds = allUserIds.stream()
                .filter(id -> !id.equals(event.ownerId()))
                .collect(Collectors.toSet());

        notificationService.createAll(
                otherUserIds,
                "새로운 의상 속성이 추가되었어요.",
                String.format("[%s] 의상에 속성이 추가되었습니다.", event.clothesAttributeDefDto().name()),
                NotificationLevel.INFO
        );
    }

    // 의상 속성 수정 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeUpdatedEvent event) {
        Set<UUID> allUserIds = userRepository.findAllUserIds();

        Set<UUID> otherUserIds = allUserIds.stream()
                .filter(id -> !id.equals(event.ownerId()))
                .collect(Collectors.toSet());

        notificationService.createAll(
                otherUserIds,
                "새로운 의상 속성이 변경되었어요.",
                String.format("[%s] 속성을 확인해보세요.", event.clothesAttributeDefDto().name()),
                NotificationLevel.INFO
        );

    }

    // 피드에 좋아요 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedLikedEvent event) {

    }

    // 피드에 댓글 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedCommentedEvent event) {

    }

    // 팔로우한 사용자의 피드 등록 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowingFeedCreatedEvent event) {

    }

    // 내가 팔로우 당함
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowedEvent event) {

    }

    // DM 수신
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DmReceivedEvent event) {

    }
}
