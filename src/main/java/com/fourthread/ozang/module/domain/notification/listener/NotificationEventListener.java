package com.fourthread.ozang.module.domain.notification.listener;

import com.fourthread.ozang.module.domain.follow.repository.FollowRepository;
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

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    // 권한 변경 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleChangedEvent event) {
        notificationService.create(
                event.userDto().id(),
                "권한이 변경 되었어요.",
                String.format("관리자가 당신의 권한을 [%s]으로 변경하였습니다.", event.userDto().role().name()),
                NotificationLevel.INFO
        );
    }

    // 의상 속성 추가 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeAddedEvent event) {

        Set<UUID> allUserIds = userRepository.findAllUserIds();

        Set<UUID> otherUserIds = allUserIds.stream()
                .filter(id -> !id.equals(event.requesterId()))
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
                .filter(id -> !id.equals(event.requesterId()))
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
        notificationService.create(
                event.feedUserId(),
                String.format("%s님이 내 피드를 좋아합니다.", event.likeByUserName()),
                event.content(),
                NotificationLevel.INFO
        );

    }

    // 피드에 댓글 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedCommentedEvent event) {
        notificationService.create(
                event.feedAuthorUserId(),
                String.format("%s님이 댓글을 달았어요.",event.commentUserName()),
                event.content(),
                NotificationLevel.INFO
        );

    }

    // 팔로우한 사용자의 피드 등록 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowingFeedCreatedEvent event) {
        Set<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.userSummary().userId());

        notificationService.createAll(
                followerIds,
                String.format("%s님이 새로운 피드를 작성했어요.",event.userSummary().name()),
                event.content(),
                NotificationLevel.INFO
        );
    }

    // 내가 팔로우 당함
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowedEvent event) {
        notificationService.create(
                event.dto().followee().userId(),
                String.format("%s님이 나를 팔로우 했어요", event.dto().follower().name()),
                "",
                NotificationLevel.INFO
        );

    }

    // DM 수신
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(DmReceivedEvent event) {

    }
}
