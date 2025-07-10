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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private UUID receiverId;
    private Notification notification;
    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        receiverId = UUID.randomUUID();
        notification = new Notification(
                receiverId,
                "알림 제목",
                "알림 내용",
                NotificationLevel.INFO
        );
        ReflectionTestUtils.setField(notification, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(
                notification,
                "createdAt",
                Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime()
        );

        notificationDto = new NotificationDto(
                notification.getId(),
                notification.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                receiverId,
                "알림 제목",
                "알림 내용",
                NotificationLevel.INFO
        );
    }

    @DisplayName("알림을 커서 기반으로 조회할 수 있다.")
    @Test
    void findAllByReceiver_success() {
        // given
        List<Notification> notifications = List.of(notification);
        List<NotificationDto> dtoList = List.of(notificationDto);

        given(notificationRepository.findAllByCondition(
                eq(receiverId), any(), any(), eq(11), eq(SortDirection.DESCENDING))
        ).willReturn(notifications);

        given(notificationRepository.countByReceiverId(receiverId)).willReturn(1);
        given(notificationMapper.toDtoList(notifications)).willReturn(dtoList);

        // when
        NotificationCursorResponse response = notificationService.findAllByReceiver(
                receiverId, null, null, 10
        );

        // then
        assertThat(response.data()).hasSize(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.sortBy()).isEqualTo("createdAt");
        assertThat(response.sortDirection()).isEqualTo("DESCENDING");
    }

    @DisplayName("알림을 생성하면 저장되고 이벤트가 발행된다.")
    @Test
    void create_success() {
        // given
        given(notificationMapper.toDto(any())).willReturn(notificationDto);

        // when
        notificationService.create(
                receiverId, "알림 제목", "알림 내용", NotificationLevel.INFO
        );

        // then
        then(notificationRepository).should().save(any());
        then(eventPublisher).should().publishEvent(any(NotificationCreatedEvent.class));
    }

    @DisplayName("여러 알림을 생성할 수 있다.")
    @Test
    void createAll_success() {
        // given
        Set<UUID> receiverIds = Set.of(receiverId, UUID.randomUUID());
        List<Notification> notifications = receiverIds.stream()
                .map(id -> new Notification(id, "제목", "내용", NotificationLevel.INFO))
                .toList();

        List<NotificationDto> dtoList = notifications.stream()
                .map(n -> new NotificationDto(
                        UUID.randomUUID(), Instant.now(), n.getReceiverId(), "제목", "내용", NotificationLevel.INFO))
                .toList();

        given(notificationMapper.toDtoList(anyList())).willReturn(dtoList);

        // when
        notificationService.createAll(receiverIds, "제목", "내용", NotificationLevel.INFO);

        // then
        then(notificationRepository).should().saveAll(anyList());
        then(eventPublisher).should().publishEvent(any(MultipleNotificationCreatedEvent.class));
    }

    @DisplayName("알림을 삭제할 수 있다.")
    @Test
    void delete_success() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(notification));

        // when
        notificationService.delete(receiverId, notificationId);

        // then
        then(notificationRepository).should().delete(notification);
    }

    @DisplayName("존재하지 않는 알림 삭제 시 예외 발생")
    @Test
    void delete_fail_not_found() {
        // given
        UUID notificationId = UUID.randomUUID();
        given(notificationRepository.findById(notificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> notificationService.delete(receiverId, notificationId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("다른 사용자의 알림을 삭제하려 하면 예외 발생")
    @Test
    void delete_fail_access_denied() {
        // given
        UUID otherUserId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification otherNotification = new Notification(
                otherUserId, "제목", "내용", NotificationLevel.INFO
        );
        ReflectionTestUtils.setField(otherNotification, "id", notificationId);

        given(notificationRepository.findById(notificationId)).willReturn(Optional.of(otherNotification));

        // when & then
        assertThatThrownBy(() -> notificationService.delete(receiverId, notificationId))
                .isInstanceOf(AccessDeniedException.class);
    }
}