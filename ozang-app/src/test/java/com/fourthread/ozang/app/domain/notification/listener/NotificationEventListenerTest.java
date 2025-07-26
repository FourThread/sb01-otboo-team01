package com.fourthread.ozang.app.domain.notification.listener;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/*
@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;

    @Mock private UserRepository userRepository;

    @Mock private FollowRepository followRepository;

    @InjectMocks private KafkaNotificationEventListener listener;

    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();

    @DisplayName("권한 변경 시 알림이 생성된다.")
    @Test
    void handleRoleChangedEvent_shouldCreateNotification() {
        UserDto userDto = new UserDto(userId, LocalDateTime.now(), "a@a.com", "홍길동", Role.ADMIN, List.of(), false);
        RoleChangedEvent event = new RoleChangedEvent(userDto, UUID.randomUUID());

        listener.consumeRoleChanged(event);

        verify(notificationService).create(
                eq(userId),
                contains("권한이 변경"),
                contains("ADMIN"),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("의상 속성 추가 시 요청자를 제외한 모든 사용자에게 알림이 생성된다.")
    @Test
    void handleClothesAttributeAddedEvent_shouldNotifyOthers() {
        ClothesAttributeDefDto attrDto = new ClothesAttributeDefDto(UUID.randomUUID(), "색상", List.of("빨강", "파랑"));
        ClothesAttributeAddedEvent event = new ClothesAttributeAddedEvent(attrDto, userId);

        when(userRepository.findAllUserIds()).thenReturn(Set.of(userId, otherUserId));

        listener.consumeClothesAttrAdded(event);

        verify(notificationService).createAll(
                eq(Set.of(otherUserId)),
                contains("의상"),
                contains("색상"),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("의상 속성 수정 시 요청자를 제외한 모든 사용자에게 알림이 생성된다.")
    @Test
    void handleClothesAttributeUpdatedEvent_shouldNotifyOthers() {
        ClothesAttributeDefDto attrDto = new ClothesAttributeDefDto(UUID.randomUUID(), "소재", List.of("면", "폴리에스터"));
        ClothesAttributeUpdatedEvent event = new ClothesAttributeUpdatedEvent(attrDto, userId);

        when(userRepository.findAllUserIds()).thenReturn(Set.of(userId, otherUserId));

        listener.consumeClothesAttrUpdated(event);

        verify(notificationService).createAll(
                eq(Set.of(otherUserId)),
                contains("속성이 변경"),
                contains("소재"),
                eq(NotificationLevel.INFO)
        );
    }


    @DisplayName("피드 좋아요 시 피드 작성자에게 알림이 생성된다.")
    @Test
    void handleFeedLikedEvent_shouldNotifyFeedOwner() {
        FeedLikedEvent event = new FeedLikedEvent(UUID.randomUUID(), userId, "좋아요를 눌렀어요", "홍길동");

        listener.consumeFeedLiked(event);

        verify(notificationService).create(
                eq(userId),
                contains("홍길동"),
                eq("좋아요를 눌렀어요"),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("피드 댓글 작성 시 피드 작성자에게 알림이 생성된다.")
    @Test
    void handleFeedCommentedEvent_shouldNotifyFeedAuthor() {
        FeedCommentedEvent event = new FeedCommentedEvent(userId, "김철수", "멋진 피드네요");

        listener.consumeFeedCommented(event);

        verify(notificationService).create(
                eq(userId),
                contains("김철수"),
                eq("멋진 피드네요"),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("팔로잉 사용자가 피드를 작성하면 팔로워에게 알림이 생성된다.")
    @Test
    void handleFollowingFeedCreatedEvent_shouldNotifyFollowers() {
        UserSummary summary = new UserSummary(userId, "나팔박", "profile.jpg");
        FollowingFeedCreatedEvent event = new FollowingFeedCreatedEvent(summary, "오늘 피드입니다");

        when(followRepository.findFollowerIdsByFolloweeId(userId)).thenReturn(Set.of(otherUserId));

        listener.consumeFollowingFeedCreated(event);

        verify(notificationService).createAll(
                eq(Set.of(otherUserId)),
                contains("나팔박"),
                eq("오늘 피드입니다"),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("내가 팔로우 당했을 때 알림이 생성된다.")
    @Test
    void handleFollowedEvent_shouldNotifyFollowee() {
        UserSummary follower = new UserSummary(otherUserId, "따봉맨", "img.jpg");
        UserSummary followee = new UserSummary(userId, "나팔박", "profile.jpg");
        FollowDto dto = new FollowDto(UUID.randomUUID(), follower, followee);
        FollowedEvent event = new FollowedEvent(dto);

        listener.consumeFollowed(event);

        verify(notificationService).create(
                eq(userId),
                contains("따봉맨"),
                eq(""),
                eq(NotificationLevel.INFO)
        );
    }

    @DisplayName("DM을 수신하면 알림이 생성된다.")
    @Test
    void handleDmReceivedEvent_shouldNotifyReceiver() {
        UserSummary sender = new UserSummary(otherUserId, "보낸이", "sender.jpg");
        UserSummary receiver = new UserSummary(userId, "받는이", "receiver.jpg");
        DirectMessageDto dmDto = new DirectMessageDto(UUID.randomUUID(), LocalDateTime.now(), sender, receiver, "안녕하세요 DM입니다");
        DmReceivedEvent event = new DmReceivedEvent(dmDto);

        listener.consumeDmReceived(event);

        verify(notificationService).create(
                eq(userId),
                contains("보낸이"),
                eq("안녕하세요 DM입니다"),
                eq(NotificationLevel.INFO)
        );
    }
}*/
