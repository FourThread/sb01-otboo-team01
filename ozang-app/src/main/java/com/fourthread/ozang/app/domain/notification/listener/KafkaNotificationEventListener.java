package com.fourthread.ozang.app.domain.notification.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.app.domain.notification.event.ClothesAttributeAddedEvent;
import com.fourthread.ozang.app.domain.notification.event.ClothesAttributeUpdatedEvent;
import com.fourthread.ozang.app.domain.notification.event.DmReceivedEvent;
import com.fourthread.ozang.app.domain.notification.event.FeedCommentedEvent;
import com.fourthread.ozang.app.domain.notification.event.FeedLikedEvent;
import com.fourthread.ozang.app.domain.notification.event.FollowedEvent;
import com.fourthread.ozang.app.domain.notification.event.FollowingFeedCreatedEvent;
import com.fourthread.ozang.app.domain.notification.event.RoleChangedEvent;
import com.fourthread.ozang.app.domain.notification.event.WeatherChangeDetectedEvent;
import com.fourthread.ozang.app.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "ozang.weather.alert.detected")
    public void consumeWeatherAlert(String payload) throws JsonProcessingException {
        WeatherChangeDetectedEvent event = objectMapper.readValue(payload, WeatherChangeDetectedEvent.class);
        notificationService.sendWeatherAlertNotification(event);
    }


    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.role_changed")
    public void consumeRoleChanged(String payload) throws JsonProcessingException {
        RoleChangedEvent event = objectMapper.readValue(payload, RoleChangedEvent.class);
        log.debug("권한 변경 이벤트 처리 시작: userId={}, newRole={}", event.userDto().id(), event.userDto().role());
        try {
            notificationService.sendRoleChangedNotification(event);
        } catch (Exception e) {
            log.error("권한 변경 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.clothes_attr_added")
    public void consumeClothesAttrAdded(String payload) throws JsonProcessingException {
        ClothesAttributeAddedEvent event = objectMapper.readValue(payload, ClothesAttributeAddedEvent.class);
        log.debug("의상 속성 추가 이벤트 처리 시작: requesterId={}, name={}", event.requesterId(), event.clothesAttributeDefDto().name());
        try {
            notificationService.sendClothesAttributeAddedNotification(event);
        } catch (Exception e) {
            log.error("의상 속성 추가 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.clothes_attr_updated")
    public void consumeClothesAttrUpdated(String payload) throws JsonProcessingException {
        ClothesAttributeUpdatedEvent event = objectMapper.readValue(payload, ClothesAttributeUpdatedEvent.class);
        log.debug("의상 속성 수정 이벤트 처리 시작: requesterId={}, name={}", event.requesterId(), event.clothesAttributeDefDto().name());
        try {
            notificationService.sendClothesAttributeUpdatedNotification(event);
        } catch (Exception e) {
            log.error("의상 속성 수정 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.feed_liked")
    public void consumeFeedLiked(String payload) throws JsonProcessingException {
        FeedLikedEvent event = objectMapper.readValue(payload, FeedLikedEvent.class);
        log.debug("피드 좋아요 이벤트 처리 시작: feedUserId={}, by={}", event.feedUserId(), event.likeByUserName());
        try {
            notificationService.sendFeedLikedNotification(event);
        } catch (Exception e) {
            log.error("피드 좋아요 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.feed_commented")
    public void consumeFeedCommented(String payload) throws JsonProcessingException {
        FeedCommentedEvent event = objectMapper.readValue(payload, FeedCommentedEvent.class);
        log.debug("피드 댓글 이벤트 처리 시작: feedAuthorUserId={}, by={}", event.feedAuthorUserId(), event.commentUserName());
        try {
            notificationService.sendFeedCommentedNotification(event);
        } catch (Exception e) {
            log.error("피드 댓글 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.following_feed_created")
    public void consumeFollowingFeedCreated(String payload) throws JsonProcessingException {
        FollowingFeedCreatedEvent event = objectMapper.readValue(payload, FollowingFeedCreatedEvent.class);
        log.debug("팔로잉 피드 작성 이벤트 처리 시작: followeeId={}, name={}", event.userSummary().userId(), event.userSummary().name());
        try {
            notificationService.sendFollowingFeedCreatedNotification(event);
        } catch (Exception e) {
            log.error("팔로잉 피드 작성 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.followed")
    public void consumeFollowed(String payload) throws JsonProcessingException {
        FollowedEvent event = objectMapper.readValue(payload, FollowedEvent.class);
        log.debug("팔로우 이벤트 처리 시작: followeeId={}, by={}", event.dto().followee().userId(), event.dto().follower().name());
        try {
            notificationService.sendFollowedNotification(event);
        } catch (Exception e) {
            log.error("팔로우 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Async("eventTaskExecutor")
    @KafkaListener(topics = "ozang.dm_received")
    public void consumeDmReceived(String payload) throws JsonProcessingException {
        DmReceivedEvent event = objectMapper.readValue(payload, DmReceivedEvent.class);
        log.debug("DM 수신 이벤트 처리 시작: receiverId={}, from={}", event.dmDto().receiver().userId(), event.dmDto().sender().name());
        try {
            notificationService.sendDmReceivedNotification(event);
        } catch (Exception e) {
            log.error("DM 수신 이벤트 처리 실패: error={}", e.getMessage(), e);
            throw e;
        }
    }
}
