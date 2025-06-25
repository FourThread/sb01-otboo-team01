package com.fourthread.ozang.module.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.dummy.Weather;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.mapper.FeedMapper;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

  @Mock
  private FeedRepository feedRepository;

  @Mock
  private FeedLikeRepository feedLikeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private WeatherRepository weatherRepository;

  @Mock
  private FeedCommentRepository feedCommentRepository;

  @Mock
  private FeedMapper feedMapper;

  @InjectMocks
  private FeedService feedService;

  UUID userId = UUID.randomUUID();
  UUID weatherId = UUID.randomUUID();
  UUID feedId = UUID.randomUUID();
  List<UUID> clothesIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

  FeedCreateRequest request = FeedCreateRequest.builder()
      .authorId(userId)
      .weatherId(weatherId)
      .clothesIds(clothesIds)
      .content("created")
      .build();

  User user;
  Feed feed;
  FeedDto expectedFeedDto;

  @BeforeEach
  void init() {
    user = new User("test", "test@mail.com", "asdfasdf");
    user.setProfile(new Profile("test", Gender.ETC, LocalDateTime.now(),
        new Location(1.0, 1.0, 1, 1, new ArrayList<>()), 10, "url"));

    feed = Feed.builder()
        .author(null)
        .weather(null)
        .content(null)
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();

    expectedFeedDto = FeedDto.builder()
        .id(UUID.randomUUID()) // Use a test ID
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .author(
            new UserSummary(user.getId(), user.getName(), user.getProfile().getProfileImageUrl()))
        .weather(null)
        .ootds(null)
        .content("created")
        .likeCount(0L)
        .commentCount(0)
        .likedByMe(false)
        .build();

  }


  @Test
  @DisplayName("피드 생성 완료")
  void create() {

    when(userRepository.findById(request.authorId()))
        .thenReturn(Optional.of(user));
    when(weatherRepository.findById(request.weatherId()))
        .thenReturn(Optional.of(new Weather()));
    when(feedMapper.toDto(any(), any()))
        .thenReturn(expectedFeedDto);

    FeedDto feedDto = feedService.registerFeed(request);

    assertThat(feedDto).isNotNull();
    assertThat(feedDto.author().name()).isEqualTo(user.getName());
    verify(userRepository).findById(any());
    verify(weatherRepository).findById(any());
  }

  @Test
  @DisplayName("피드 생성 실패 - 유저 정보 없음")
  void createFailCauseUserNotFound() {

    when(userRepository.findById(request.authorId()))
        .thenThrow(new RuntimeException());

    assertThatThrownBy(() -> feedService.registerFeed(request))
        .isInstanceOf(RuntimeException.class);

    verify(feedRepository, never()).save(any());
  }

  @Test
  @DisplayName("피드 생성 실패 - 날씨 정보 없음")
  void createFailCauseWeatherDataNotFound() {

    assertThatThrownBy(() -> feedService.registerFeed(request))
        .isInstanceOf(RuntimeException.class);

    verify(feedRepository, never()).save(any());
  }

  @Test
  @DisplayName("피드 좋아요")
  void like() {

    when(feedRepository.findById(any()))
        .thenReturn(Optional.of(feed));

    feedService.like(feedId);

    assertThat(feed.getLikeCount().intValue()).isEqualTo(1);
  }

  @Test
  @DisplayName("피드 좋아요 실패 - 피드가 없음")
  void failLike() {

    when(feedRepository.findById(any()))
        .thenThrow(new FeedNotFoundException(null, null, null));

    assertThatThrownBy(() -> feedService.like(any()))
        .isInstanceOf(FeedNotFoundException.class);
  }
}