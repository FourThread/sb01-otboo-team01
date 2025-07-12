package com.fourthread.ozang.module.domain.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fourthread.ozang.module.domain.feed.dto.FeedCommentData;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.dto.FeedData;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedUpdateRequest;
import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.feed.entity.SortBy;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.mapper.FeedMapper;
import com.fourthread.ozang.module.domain.feed.repository.FeedClothesRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeedServiceTest {

  @Mock
  private FeedElasticsearchRepository feedElasticsearchRepository;

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
  private FeedClothesRepository feedClothesRepository;

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
    user.setProfile(new Profile("test", Gender.ETC, LocalDate.now(),
        new Location(1.0, 1.0, 1, 1, new ArrayList<>()), 10, "url"));

    feed = Feed.builder()
        .author(user)
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

  @Disabled
  @Test
  @DisplayName("피드 생성 완료")
  void create() {

    when(userRepository.findById(request.authorId()))
        .thenReturn(Optional.of(user));
    when(weatherRepository.findById(request.weatherId()))
        .thenReturn(Optional.of((Weather) new Object()));
    when(feedMapper.toDto(any(), any(), any(), any()))
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
  @DisplayName("피드 목록 조회 실패 - request가 잘못 들어옴")
  void retrieveFeedNullRequest() {
    assertThrows(IllegalArgumentException.class, () -> feedService.retrieveFeed(null, null));
  }

  @Test
  @DisplayName("피드 목록 조회 - 커서 없음")
  void retrieveFeedNoNext() {
    FeedPaginationRequest request = mock(FeedPaginationRequest.class);
    when(request.limit()).thenReturn(2);
    when(request.sortBy()).thenReturn(SortBy.createdAt);
    when(request.sortDirection()).thenReturn(SortDirection.ASCENDING);

    FeedDto dto1 = mock(FeedDto.class);
    FeedDto dto2 = mock(FeedDto.class);
    List<FeedDto> data = List.of(dto1, dto2);

    when(feedRepository.search(request, null)).thenReturn(data);
    when(feedRepository.feedTotalCount(request)).thenReturn(2L);

    FeedData result = feedService.retrieveFeed(request, null);

    assertFalse(result.hasNext());
    assertEquals(data, result.data());
    assertNull(result.nextCursor());
    assertNull(result.nextIdAfter());
    assertEquals(2L, result.totalCount());
    assertEquals(SortBy.createdAt, result.sortBy());
    assertEquals(SortDirection.ASCENDING, result.sortDirection());

    verify(feedRepository).search(request, null);
    verify(feedRepository).feedTotalCount(request);
  }

  @Test
  @DisplayName("피드 목록 조회 - 커서 존재")
  void retrieveFeedHasNext() {
    // 준비
    FeedPaginationRequest request = mock(FeedPaginationRequest.class);
    when(request.limit()).thenReturn(1);
    when(request.sortBy()).thenReturn(SortBy.createdAt);
    when(request.sortDirection()).thenReturn(SortDirection.DESCENDING);

    // 두 개 이상의 복제 가능한 FeedDto 생성
    UUID id1 = UUID.randomUUID();
    LocalDateTime ts1 = LocalDateTime.now();

    FeedDto dto1 = mock(FeedDto.class);
    FeedDto dto2 = mock(FeedDto.class);
    when(dto1.id()).thenReturn(id1);
    when(dto1.createdAt()).thenReturn(ts1);
    // dto2 데이터는 실제로 페이징에 사용되지 않으므로 액세서만 더미로 설정
    when(dto2.id()).thenReturn(UUID.randomUUID());
    when(dto2.createdAt()).thenReturn(ts1.plusMinutes(1));

    List<FeedDto> data = List.of(dto1, dto2);
    when(feedRepository.search(request, userId)).thenReturn(data);
    when(feedRepository.feedTotalCount(request)).thenReturn(2L);

    // 실행
    FeedData result = feedService.retrieveFeed(request, userId);

    // 검증
    assertTrue(result.hasNext());
    assertEquals(List.of(dto1), result.data());
    assertEquals(ts1.toString(), result.nextCursor());
    assertEquals(id1, result.nextIdAfter());
    assertEquals(2L, result.totalCount());
    assertEquals(SortBy.createdAt, result.sortBy());
    assertEquals(SortDirection.DESCENDING, result.sortDirection());

    verify(feedRepository).search(request, userId);
    verify(feedRepository).feedTotalCount(request);
  }

  @Test
  @DisplayName("피드 목록 조회 - 좋아요한 사용자 ID로 조회")
  void retrieveFeedWithLikeByUserId() {
    // 준비
    FeedPaginationRequest request = mock(FeedPaginationRequest.class);
    when(request.limit()).thenReturn(2);
    when(request.sortBy()).thenReturn(SortBy.createdAt);
    when(request.sortDirection()).thenReturn(SortDirection.DESCENDING);

    FeedDto dto1 = mock(FeedDto.class);
    FeedDto dto2 = mock(FeedDto.class);
    List<FeedDto> data = List.of(dto1, dto2);

    UUID likeByUserId = UUID.randomUUID();
    when(feedRepository.search(request, likeByUserId)).thenReturn(data);
    when(feedRepository.feedTotalCount(request)).thenReturn(2L);

    // 실행
    FeedData result = feedService.retrieveFeed(request, likeByUserId);

    // 검증
    assertFalse(result.hasNext());
    assertEquals(data, result.data());
    assertEquals(2L, result.totalCount());

    verify(feedRepository).search(request, likeByUserId);
    verify(feedRepository).feedTotalCount(request);
  }

  @Test
  @DisplayName("피드 삭제")
  void delete() {

    when(feedMapper.toDto(feed, feed.getAuthor(), feed.getWeather(), null))
        .thenReturn(expectedFeedDto);
    when(feedRepository.findById(feedId))
        .thenReturn(Optional.of(feed));

    feedService.delete(feedId);

    verify(feedRepository).delete(any());
    verify(feedLikeRepository).deleteAllByFeed_Id(feedId);
  }

  @Test
  @DisplayName("피드 삭제 실패")
  void failDelete() {

    assertThatThrownBy(() -> feedService.delete(feedId))
        .isInstanceOf(FeedNotFoundException.class);

    verify(feedRepository, never()).delete(any());

  }

  @Test
  @DisplayName("피드 수정")
  void update() {

    FeedUpdateRequest newContent = new FeedUpdateRequest("new content");

    ArgumentCaptor<Feed> captor = ArgumentCaptor.forClass(Feed.class);
    when(feedMapper.toDto(captor.capture(), any(User.class), any(), any()))
        .thenReturn(expectedFeedDto);
    when(feedRepository.findById(any()))
        .thenReturn(Optional.of(feed));

    FeedDto updatedFeed = feedService.update(feedId, newContent);

    assertThat(updatedFeed).isNotNull();
    assertThat(captor.getValue().getContent())
        .isEqualTo(newContent.content());
  }

  @Test
  @DisplayName("피드 좋아요")
  void like() {

    when(feedRepository.findById(any()))
        .thenReturn(Optional.of(feed));
    when(feedMapper.toDto(any(), any(), any(), any()))
        .thenReturn(expectedFeedDto);

    FeedDto result = feedService.like(feedId, userId);

    assertThat(feed.getLikeCount().intValue()).isEqualTo(1);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("피드 좋아요 - likeByUserId null")
  void likeWithNullUserId() {

    when(feedRepository.findById(any()))
        .thenReturn(Optional.of(feed));
    when(feedMapper.toDto(any(), any(), any(), any()))
        .thenReturn(expectedFeedDto);

    FeedDto result = feedService.like(feedId, null);

    assertThat(feed.getLikeCount().intValue()).isEqualTo(1);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("피드 좋아요 실패 - 피드가 없음")
  void failLike() {

    when(feedRepository.findById(any()))
        .thenThrow(new FeedNotFoundException(null, null, null));

    assertThatThrownBy(() -> feedService.like(feedId, userId))
        .isInstanceOf(FeedNotFoundException.class);
  }

  @Test
  @DisplayName("피드 댓글 등록")
  void postComment() {

    CommentCreateRequest commentCreateRequest = new CommentCreateRequest(feedId, userId, "댓글");

    when(feedRepository.findById(feedId))
        .thenReturn(Optional.of(feed));
    when(userRepository.findById(userId))
        .thenReturn(Optional.of(user));

    feedService.postComment(commentCreateRequest);

    ArgumentCaptor<FeedComment> captor = ArgumentCaptor.forClass(
        FeedComment.class);
    verify(feedCommentRepository).save(captor.capture());

    FeedComment comment = captor.getValue();
    assertThat("댓글").isEqualTo(comment.getContent());
  }

  @Test
  @DisplayName("댓글 목록 조회 - 커서 없음")
  void retrieveCommentNoNext() {
    CommentPaginationRequest req = mock(CommentPaginationRequest.class);
    when(req.limit()).thenReturn(2);
    when(req.cursor()).thenReturn(null);
    when(req.idAfter()).thenReturn(null);
    when(req.feedId()).thenReturn(UUID.randomUUID());

    FeedCommentDto dto1 = new FeedCommentDto(UUID.randomUUID(), LocalDateTime.now(), feed.getId(), null, "test");
    FeedCommentDto dto2 = new FeedCommentDto(UUID.randomUUID(), LocalDateTime.now(), feed.getId(), null, "test2");
    List<FeedCommentDto> list = List.of(dto1, dto2);

    when(feedCommentRepository.searchComment(req)).thenReturn(list);
    when(feedCommentRepository.commentTotalCount(req)).thenReturn(2L);

    FeedCommentData result = feedService.retrieveComment(req);

    assertFalse(result.hasNext());
    assertEquals(list, result.data());
    assertNull(result.nextCursor());
    assertNull(result.nextIdAfter());
    assertEquals(2L, result.totalCount());

    verify(feedCommentRepository).searchComment(req);
    verify(feedCommentRepository).commentTotalCount(req);
  }

  @Test
  @DisplayName("댓글 목록 조회 - 커서 있음, 페이징 다음 페이지 존재")
  void retrieveCommentWithNext() {
    CommentPaginationRequest req = mock(CommentPaginationRequest.class);
    when(req.limit()).thenReturn(2);
    when(req.cursor()).thenReturn("2025-06-30T10:00:00");
    UUID afterId = UUID.randomUUID();
    when(req.idAfter()).thenReturn(afterId);
    when(req.feedId()).thenReturn(UUID.randomUUID());

    LocalDateTime base = LocalDateTime.parse("2025-06-30T10:00:00");
    FeedCommentDto dto1 = new FeedCommentDto(UUID.randomUUID(), LocalDateTime.now(), feed.getId(), null, "test1");
    FeedCommentDto dto2 = new FeedCommentDto(UUID.randomUUID(), LocalDateTime.now(), feed.getId(), null, "test2");
    FeedCommentDto dto3 = new FeedCommentDto(UUID.randomUUID(), LocalDateTime.now(), feed.getId(), null, "test3");
    List<FeedCommentDto> list = List.of(dto1, dto2, dto3);

    when(feedCommentRepository.searchComment(req)).thenReturn(list);
    when(feedCommentRepository.commentTotalCount(req)).thenReturn(5L);

    FeedCommentData result = feedService.retrieveComment(req);

    assertTrue(result.hasNext());
    assertEquals(2, result.data().size());
    assertEquals(dto2.createdAt().toString(), result.nextCursor());
    assertEquals(dto2.id(), result.nextIdAfter());
    assertEquals(5L, result.totalCount());

    verify(feedCommentRepository).searchComment(req);
    verify(feedCommentRepository).commentTotalCount(req);
  }
}