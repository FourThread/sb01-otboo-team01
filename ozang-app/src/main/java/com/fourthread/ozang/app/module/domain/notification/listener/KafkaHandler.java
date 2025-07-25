package com.fourthread.ozang.module.domain.notification.listener;

import com.fourthread.ozang.module.domain.notification.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHandler {

    private final KafkaNotificationProducer kafkaProducer;

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WeatherChangeDetectedEvent event) {
        kafkaProducer.send("ozang.weather.alert.detected", event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RoleChangedEvent event) {
        kafkaProducer.send("ozang.role_changed", event.userDto().id(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeAddedEvent event) {
        kafkaProducer.send("ozang.clothes_attr_added", event.requesterId(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ClothesAttributeUpdatedEvent event) {
        kafkaProducer.send("ozang.clothes_attr_updated", event.requesterId(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedLikedEvent event) {
        kafkaProducer.send("ozang.feed_liked", event.feedUserId(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FeedCommentedEvent event) {
        kafkaProducer.send("ozang.feed_commented", event.feedAuthorUserId(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowingFeedCreatedEvent event) {
        kafkaProducer.send("ozang.following_feed_created", event.userSummary().userId(), event);
    }

    @Async("eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FollowedEvent event) {
        UUID receiverId = event.dto().followee().userId();
        kafkaProducer.send("ozang.followed", receiverId, event);
    }

    @Async("eventTaskExecutor")
    @EventListener
    public void handle(DmReceivedEvent event) {
        UUID receiverId = event.dmDto().receiver().userId();
        kafkaProducer.send("ozang.dm_received", receiverId, event);
    }
}
