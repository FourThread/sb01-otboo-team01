package com.fourthread.ozang.module.domain.feed.repository;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.user.dto.type.Gender;
import com.fourthread.ozang.module.domain.user.dto.type.Location;
import com.fourthread.ozang.module.domain.user.entity.Profile;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "ADMIN_USERNAME=test-admin",
    "ADMIN_EMAIL=test-admin@mail.com",
    "ADMIN_PASSWORD=test-pass",
    "JWT_SECRET=d12d12d21d21d12d2",
    "KAKAO_API_KEY=test",
    "WEATHER_API_KEY=dwqqdd11"
})
class FeedCommentRepositoryTest {

  @Autowired
  private FeedRepository feedRepository;

  @Autowired
  private FeedCommentRepository feedCommentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private WeatherRepository weatherRepository;

  @Autowired
  private ClothesRepository clothesRepository;

  @Autowired
  private ClothesAttributeDefinitionRepository attributeDefRepository;

  @Autowired
  private FeedClothesRepository feedClothesRepository;

  private User author, commenter1, commenter2;
  private Weather weather;
  private Clothes clothes;
  private Feed targetFeed, otherFeed;

  @BeforeEach
  void setUp() {
    setupUsers();
    setupWeatherAndClothes();
    setupFeeds();
  }

  private void setupUsers() {
    // 피드 작성자
    author = userRepository.save(new User("author", "author@example.com", "password"));
    Profile authorProfile = new Profile("author", Gender.MALE, LocalDate.now().minusYears(25),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 3, "author.jpg");
    authorProfile.setUser(author);
    profileRepository.save(authorProfile);

    // 댓글 작성자1
    commenter1 = userRepository.save(new User("commenter1", "commenter1@example.com", "password"));
    Profile commenter1Profile = new Profile("commenter1", Gender.FEMALE, LocalDate.now().minusYears(23),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 2, "commenter1.jpg");
    commenter1Profile.setUser(commenter1);
    profileRepository.save(commenter1Profile);

    // 댓글 작성자2
    commenter2 = userRepository.save(new User("commenter2", "commenter2@example.com", "password"));
    Profile commenter2Profile = new Profile("Commenter Two", Gender.MALE, LocalDate.now().minusYears(27),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 1, "commenter2.jpg");
    commenter2Profile.setUser(commenter2);
    profileRepository.save(commenter2Profile);
  }

  private void setupWeatherAndClothes() {
    WeatherAPILocation location = new WeatherAPILocation(
        37.5665, 126.9780, 60, 127, List.of("Seoul", "Gangnam-gu")
    );

    weather = weatherRepository.save(
        Weather.create(LocalDateTime.now().minusHours(1), LocalDateTime.now(), location, SkyStatus.CLEAR)
    );

    clothes = clothesRepository.save(Clothes.builder()
        .name("T-shirt")
        .ownerId(author.getId())
        .imageUrl("tshirt.jpg")
        .type(null)
        .build());

    ClothesAttributeDefinition definition = attributeDefRepository.save(
        new ClothesAttributeDefinition("color", List.of("red", "blue", "black"))
    );
  }

  private void setupFeeds() {
    // 타겟 피드 (댓글을 달 피드)
    targetFeed = Feed.builder()
        .author(author)
        .weather(weather)
        .content("Target feed for comments")
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();
    targetFeed = feedRepository.save(targetFeed);
    feedClothesRepository.save(new FeedClothes(clothes, targetFeed));

    // 다른 피드 (댓글 필터링 테스트용)
    otherFeed = Feed.builder()
        .author(author)
        .weather(weather)
        .content("Other feed")
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();
    otherFeed = feedRepository.save(otherFeed);
    feedClothesRepository.save(new FeedClothes(clothes, otherFeed));
  }

  @Test
  @DisplayName("특정 피드의 모든 댓글 조회")
  void searchComment_allComments() {
    // Given
    createTestComments(targetFeed, 5);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(5);
    assertThat(result).isSortedAccordingTo((a, b) -> a.createdAt().compareTo(b.createdAt()));
    assertThat(result).allMatch(comment -> comment.feedId().equals(targetFeed.getId()));
  }

  @Test
  @DisplayName("피드별 댓글 필터링 - 다른 피드의 댓글은 조회되지 않음")
  void searchComment_filterByFeed() {
    // Given
    createTestComments(targetFeed, 3);
    createTestComments(otherFeed, 2);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result).allMatch(comment -> comment.feedId().equals(targetFeed.getId()));
  }

  @Test
  @DisplayName("댓글 페이지 제한 테스트")
  void searchComment_withLimit() {
    // Given
    createTestComments(targetFeed, 10);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(3)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(4); // limit + 1 for hasNext check
  }

  @Test
  @DisplayName("댓글 커서 기반 페이징")
  void searchComment_cursorPaging() throws InterruptedException {
    // Given
    List<FeedComment> comments = createTestCommentsWithTimeGap(targetFeed, 5);

    // 첫 번째 페이지
    CommentPaginationRequest firstRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(2)
        .build();

    List<FeedCommentDto> firstPage = feedCommentRepository.searchComment(firstRequest);

    // 두 번째 페이지
    FeedCommentDto lastCommentFromFirstPage = firstPage.get(1); // limit만큼만 사용
    CommentPaginationRequest secondRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(lastCommentFromFirstPage.createdAt().toString())
        .idAfter(lastCommentFromFirstPage.id())
        .limit(2)
        .build();

    // When
    List<FeedCommentDto> secondPage = feedCommentRepository.searchComment(secondRequest);

    // Then
    assertThat(firstPage).hasSize(3); // limit + 1
    assertThat(secondPage).hasSize(3); // limit + 1

    // 첫 번째 페이지와 두 번째 페이지의 데이터가 겹치지 않는지 확인
    List<String> firstPageIds = firstPage.stream()
        .limit(2) // 실제 limit만큼만
        .map(comment -> comment.id().toString())
        .toList();
    List<String> secondPageIds = secondPage.stream()
        .limit(2) // 실제 limit만큼만
        .map(comment -> comment.id().toString())
        .toList();

    assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);

    // 시간 순서 확인 (ASCENDING)
    assertThat(firstPage.get(0).createdAt()).isBefore(firstPage.get(1).createdAt());
    assertThat(secondPage.get(0).createdAt()).isAfter(firstPage.get(1).createdAt());
  }

  @Test
  @DisplayName("댓글 없는 피드의 댓글 조회")
  void searchComment_noComments() {
    // Given
    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("댓글 작성자 정보 포함 확인")
  void searchComment_withAuthorInfo() {
    // Given
    createCommentWithContent(targetFeed, commenter1, "First comment");
    createCommentWithContent(targetFeed, commenter2, "Second comment");

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(2);

    FeedCommentDto firstComment = result.get(0);
    assertThat(firstComment.author()).isNotNull();
    assertThat(firstComment.author().userId()).isEqualTo(commenter1.getId());
    assertThat(firstComment.author().name()).isEqualTo("commenter1");
    assertThat(firstComment.author().profileImageUrl()).isEqualTo("commenter1.jpg");
    assertThat(firstComment.content()).isEqualTo("First comment");

    FeedCommentDto secondComment = result.get(1);
    assertThat(secondComment.author()).isNotNull();
    assertThat(secondComment.author().userId()).isEqualTo(commenter2.getId());
    assertThat(secondComment.author().name()).isEqualTo("commenter2");
    assertThat(secondComment.content()).isEqualTo("Second comment");
  }

  @Test
  @DisplayName("전체 댓글 개수 조회")
  void commentTotalCount_all() {
    // Given
    createTestComments(targetFeed, 7);
    createTestComments(otherFeed, 3); // 다른 피드의 댓글

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .build();

    // When
    Long totalCount = feedCommentRepository.commentTotalCount(request);

    // Then
    assertThat(totalCount).isEqualTo(7); // 타겟 피드의 댓글만 카운트
  }

  @Test
  @DisplayName("댓글 없는 피드의 댓글 개수 조회")
  void commentTotalCount_noComments() {
    // Given
    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .build();

    // When
    Long totalCount = feedCommentRepository.commentTotalCount(request);

    // Then
    assertThat(totalCount).isEqualTo(0);
  }

  @Test
  @DisplayName("커서 없이 페이징 요청")
  void searchComment_noCursor() {
    // Given
    createTestComments(targetFeed, 5);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(3)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(4); // limit + 1
    assertThat(result).isSortedAccordingTo((a, b) -> a.createdAt().compareTo(b.createdAt()));
  }

  @Test
  @DisplayName("다양한 사용자의 댓글이 시간순으로 정렬됨")
  void searchComment_mixedAuthors_sortedByTime() throws InterruptedException {
    // Given
    createCommentWithContent(targetFeed, commenter1, "First by commenter1");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, commenter2, "First by commenter2");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, commenter1, "Second by commenter1");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, author, "Reply by author");

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    List<FeedCommentDto> result = feedCommentRepository.searchComment(request);

    // Then
    assertThat(result).hasSize(4);
    assertThat(result.get(0).content()).isEqualTo("First by commenter1");
    assertThat(result.get(1).content()).isEqualTo("First by commenter2");
    assertThat(result.get(2).content()).isEqualTo("Second by commenter1");
    assertThat(result.get(3).content()).isEqualTo("Reply by author");

    // 시간 순서 확인
    for (int i = 0; i < result.size() - 1; i++) {
      assertThat(result.get(i).createdAt()).isBefore(result.get(i + 1).createdAt());
    }
  }

  // Helper methods
  private List<FeedComment> createTestComments(Feed feed, int count) {
    List<FeedComment> comments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User commenter = i % 2 == 0 ? commenter1 : commenter2;
      FeedComment comment = createCommentWithContent(feed, commenter, "Test comment " + i);
      comments.add(comment);
    }
    return comments;
  }

  private List<FeedComment> createTestCommentsWithTimeGap(Feed feed, int count) throws InterruptedException {
    List<FeedComment> comments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User commenter = i % 2 == 0 ? commenter1 : commenter2;
      FeedComment comment = createCommentWithContent(feed, commenter, "Test comment " + i);
      comments.add(comment);
      if (i < count - 1) {
        Thread.sleep(100); // 시간 간격
      }
    }
    return comments;
  }

  private FeedComment createCommentWithContent(Feed feed, User commenter, String content) {
    FeedComment comment = FeedComment.builder()
        .feed(feed)
        .author(commenter)
        .content(content)
        .build();

    return feedCommentRepository.save(comment);
  }
}