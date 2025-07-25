package com.fourthread.ozang.domain.notification.listener;

import com.fourthread.ozang.module.domain.follow.repository.FollowRepository;
import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.service.NotificationService;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    // 날씨 변화 감지 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WeatherChangeDetectedEvent event) {
        log.info("날씨 변화 감지 이벤트 처리 시작 - 변화 수: {}", event.weatherChanges().size());

        // 이벤트 로깅 및 추가 처리 로직
        event.weatherChanges().forEach(change -> {
            log.info("날씨 변화 감지: {} - {}",
                change.changeType().getDescription(), change.description());
        });

        // 추가적인 비즈니스 로직 (통계, 모니터링 등)
    }

    // 권한 변경 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleChangedEvent event) {
        UUID receiverId = event.userDto().id();
        log.debug("권한 변경 이벤트 처리 시작: userId={}, newRole={}", receiverId, event.userDto().role());
        try {
            notificationService.create(
                    event.userDto().id(),
                    "권한이 변경 되었어요.",
                    String.format("관리자가 당신의 권한을 [%s]으로 변경하였습니다.", event.userDto().role().name()),
                    NotificationLevel.INFO
            );
            log.info("권한 변경 이벤트 처리 완료: receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("권한 변경 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    // 의상 속성 추가 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeAddedEvent event) {
        log.debug("의상 속성 추가 이벤트 처리 시작: requesterId={}, name={}", event.requesterId(), event.clothesAttributeDefDto().name());
        try {
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
            log.info("의상 속성 추가 이벤트 처리 완료: count={}", otherUserIds.size());
        } catch (Exception e) {
            log.error("의상 속성 추가 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }

    }

    // 의상 속성 수정 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeUpdatedEvent event) {
        log.debug("의상 속성 수정 이벤트 처리 시작: requesterId={}, name={}", event.requesterId(), event.clothesAttributeDefDto().name());
        try {
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
            log.info("의상 속성 수정 이벤트 처리 완료: count={}", otherUserIds.size());
        } catch (Exception e){
            log.error("의상 속성 수정 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    // 피드에 좋아요 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedLikedEvent event) {
        log.debug("피드 좋아요 이벤트 처리 시작: feedUserId={}, by={}", event.feedUserId(), event.likeByUserName());
        try {
            notificationService.create(
                    event.feedUserId(),
                    String.format("%s님이 내 피드를 좋아합니다.", event.likeByUserName()),
                    event.content(),
                    NotificationLevel.INFO
            );
            log.info("피드 좋아요 이벤트 처리 완료: receiverId={}", event.feedUserId());
        } catch (Exception e) {
            log.error("피드 좋아요 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    // 피드에 댓글 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedCommentedEvent event) {
        log.debug("피드 댓글 이벤트 처리 시작: feedAuthorUserId={}, by={}", event.feedAuthorUserId(), event.commentUserName());
        try {
            notificationService.create(
                    event.feedAuthorUserId(),
                    String.format("%s님이 댓글을 달았어요.",event.commentUserName()),
                    event.content(),
                    NotificationLevel.INFO
            );
            log.info("피드 댓글 이벤트 처리 완료: receiverId={}", event.feedAuthorUserId());
        } catch (Exception e) {
            log.error("피드 댓글 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }

    }

    // 팔로우한 사용자의 피드 등록 이벤트
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowingFeedCreatedEvent event) {
        UUID followeeId = event.userSummary().userId();
        log.debug("팔로잉 피드 작성 이벤트 처리 시작: followeeId={}, name={}", followeeId, event.userSummary().name());
        try {
            Set<UUID> followerIds = followRepository.findFollowerIdsByFolloweeId(event.userSummary().userId());

            notificationService.createAll(
                    followerIds,
                    String.format("%s님이 새로운 피드를 작성했어요.",event.userSummary().name()),
                    event.content(),
                    NotificationLevel.INFO
            );
            log.info("팔로잉 피드 작성 이벤트 처리 완료: count={}", followerIds.size());
        } catch (Exception e) {
            log.error("팔로잉 피드 작성 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    // 내가 팔로우 당함
    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowedEvent event) {
        UUID receiverId = event.dto().followee().userId();
        String followerName = event.dto().follower().name();
        log.debug("팔로우 이벤트 처리 시작: followeeId={}, by={}", receiverId, followerName);
        try {
            notificationService.create(
                    event.dto().followee().userId(),
                    String.format("%s님이 나를 팔로우 했어요", event.dto().follower().name()),
                    "",
                    NotificationLevel.INFO
            );
            log.info("팔로우 이벤트 처리 완료: receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("팔로우 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }

    }

    // DM 수신
    @Async("eventTaskExecutor")
    @EventListener
    public void handle(DmReceivedEvent event) {
        UUID receiverId = event.dmDto().receiver().userId();
        String senderName = event.dmDto().sender().name();
        log.debug("DM 수신 이벤트 처리 시작: receiverId={}, from={}", receiverId, senderName);
        try {
            notificationService.create(
                    event.dmDto().receiver().userId(),
                    String.format("[DM] %s", event.dmDto().sender().name()),
                    event.dmDto().content(),
                    NotificationLevel.INFO
            );
            log.info("DM 수신 이벤트 처리 완료: receiverId={}", receiverId);
        } catch (Exception e) {
            log.error("DM 수신 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }
}
