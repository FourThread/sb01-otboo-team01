package com.fourthread.ozang.module.domain.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentData;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.feed.repository.FeedClothesRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.feed.service.FeedService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "ADMIN_USERNAME=test-admin",
        "ADMIN_EMAIL=test-admin@mail.com",
        "ADMIN_PASSWORD=test-pass",
        "JWT_SECRET=d12d12d21d21d12d2",
        "KAKAO_API_KEY=test",
        "WEATHER_API_KEY=dwqqdd11",
        "cloud.aws.credentials.access-key=testAccessKey",
        "cloud.aws.credentials.secret-key=testSecretKey",
        "cloud.aws.region.static=ap-northeast-2"
    }
)
@TestPropertySource(properties = {
    "AWS_ACCESS_KEY=testAccessKey",
    "AWS_SECRET_KEY=testSecretKey",
    "cloud.aws.region.static=ap-northeast-2"
})
class FeedCommentServiceIntegrationTest {

  @Autowired
  private FeedService feedCommentService;

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

  private User author, commenter1, commenter2, commenter3;
  private Weather weather;
  private Clothes clothes;
  private Feed targetFeed, anotherFeed;

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

    // 댓글 작성자들
    commenter1 = userRepository.save(new User("commenter1", "commenter1@example.com", "password"));
    Profile commenter1Profile = new Profile("commenter1", Gender.FEMALE, LocalDate.now().minusYears(23),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 2, "commenter1.jpg");
    commenter1Profile.setUser(commenter1);
    profileRepository.save(commenter1Profile);

    commenter2 = userRepository.save(new User("commenter2", "commenter2@example.com", "password"));
    Profile commenter2Profile = new Profile("Commenter Two", Gender.MALE, LocalDate.now().minusYears(27),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 1, "commenter2.jpg");
    commenter2Profile.setUser(commenter2);
    profileRepository.save(commenter2Profile);

    commenter3 = userRepository.save(new User("commenter3", "commenter3@example.com", "password"));
    Profile commenter3Profile = new Profile("Commenter Three", Gender.ETC, LocalDate.now().minusYears(22),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 4, "commenter3.jpg");
    commenter3Profile.setUser(commenter3);
    profileRepository.save(commenter3Profile);
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
    // 타겟 피드
    targetFeed = Feed.builder()
        .author(author)
        .weather(weather)
        .content("Target feed for comments testing")
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();
    targetFeed = feedRepository.save(targetFeed);
    feedClothesRepository.save(new FeedClothes(clothes, targetFeed));

    // 다른 피드
    anotherFeed = Feed.builder()
        .author(author)
        .weather(weather)
        .content("Another feed")
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();
    anotherFeed = feedRepository.save(anotherFeed);
    feedClothesRepository.save(new FeedClothes(clothes, anotherFeed));
  }

  @Test
  @DisplayName("댓글 조회 기본 기능")
  void retrieveComments_basic() {
    // Given
    createTestComments(targetFeed, 10);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(5)
        .build();

    // When
    FeedCommentData result = feedCommentService.retrieveComment(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(5);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isNotNull();
    assertThat(result.nextIdAfter()).isNotNull();
    assertThat(result.totalCount()).isEqualTo(10);
  }

  @Test
  @DisplayName("댓글 페이징 (다음 페이지 없음)")
  void retrieveComments_noNext() {
    // Given
    createTestComments(targetFeed, 3);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(5)
        .build();

    // When
    FeedCommentData result = feedCommentService.retrieveComment(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(3);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("댓글 커서 기반 페이징")
  void retrieveComments_cursorPaging() throws InterruptedException {
    // Given
    createTestCommentsWithTimeGap(targetFeed, 10);

    // 첫 번째 페이지
    CommentPaginationRequest firstRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(4)
        .build();

    FeedCommentData firstResult = feedCommentService.retrieveComment(firstRequest);

    // When - 두 번째 페이지
    CommentPaginationRequest secondRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(firstResult.nextCursor())
        .idAfter(firstResult.nextIdAfter())
        .limit(4)
        .build();

    FeedCommentData secondResult = feedCommentService.retrieveComment(secondRequest);

    // Then
    assertThat(firstResult.data()).hasSize(4);
    assertThat(firstResult.hasNext()).isTrue();
    assertThat(firstResult.totalCount()).isEqualTo(10);

    assertThat(secondResult.data()).hasSize(4);
    assertThat(secondResult.hasNext()).isTrue();
    assertThat(secondResult.totalCount()).isEqualTo(10);

    // 첫 번째와 두 번째 페이지가 다른 댓글들인지 확인
    List<String> firstPageIds = firstResult.data().stream()
        .map(comment -> comment.id().toString())
        .toList();
    List<String> secondPageIds = secondResult.data().stream()
        .map(comment -> comment.id().toString())
        .toList();

    assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);

    // 시간 순서 확인
    FeedCommentDto lastFromFirst = firstResult.data().get(3);
    FeedCommentDto firstFromSecond = secondResult.data().get(0);
    assertThat(lastFromFirst.createdAt()).isBefore(firstFromSecond.createdAt());
  }

  @Test
  @DisplayName("댓글 없는 피드")
  void retrieveComments_emptyFeed() {
    // Given
    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    FeedCommentData result = feedCommentService.retrieveComment(request);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.data()).isEmpty();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(0);
  }

  @Test
  @DisplayName("피드별 댓글 분리")
  void retrieveComments_separateByFeed() {
    // Given
    createTestComments(targetFeed, 5);
    createTestComments(anotherFeed, 3);

    CommentPaginationRequest targetRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    CommentPaginationRequest anotherRequest = CommentPaginationRequest.builder()
        .feedId(anotherFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    FeedCommentData targetResult = feedCommentService.retrieveComment(targetRequest);
    FeedCommentData anotherResult = feedCommentService.retrieveComment(anotherRequest);

    // Then
    assertThat(targetResult.data()).hasSize(5);
    assertThat(targetResult.totalCount()).isEqualTo(5);
    assertThat(targetResult.data()).allMatch(comment ->
        comment.feedId().equals(targetFeed.getId()));

    assertThat(anotherResult.data()).hasSize(3);
    assertThat(anotherResult.totalCount()).isEqualTo(3);
    assertThat(anotherResult.data()).allMatch(comment ->
        comment.feedId().equals(anotherFeed.getId()));
  }

  @Test
  @DisplayName("다양한 작성자의 댓글 혼재")
  void retrieveComments_mixedAuthors() throws InterruptedException {
    // Given - 다양한 작성자가 댓글 작성
    createCommentWithContent(targetFeed, commenter1, "첫 번째 댓글");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, author, "작성자의 댓글");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, commenter2, "두 번째 댓글");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, commenter3, "세 번째 댓글");
    Thread.sleep(100);
    createCommentWithContent(targetFeed, commenter1, "다시 첫 번째 작성자");

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    FeedCommentData result = feedCommentService.retrieveComment(request);

    // Then
    assertThat(result.data()).hasSize(5);
    assertThat(result.totalCount()).isEqualTo(5);

    List<FeedCommentDto> comments = result.data();
    assertThat(comments.get(0).content()).isEqualTo("첫 번째 댓글");
    assertThat(comments.get(0).author().name()).isEqualTo("commenter1");

    assertThat(comments.get(1).content()).isEqualTo("작성자의 댓글");
    assertThat(comments.get(1).author().name()).isEqualTo("author");

    assertThat(comments.get(2).content()).isEqualTo("두 번째 댓글");
    assertThat(comments.get(2).author().name()).isEqualTo("commenter2");

    assertThat(comments.get(3).content()).isEqualTo("세 번째 댓글");
    assertThat(comments.get(3).author().name()).isEqualTo("commenter3");

    assertThat(comments.get(4).content()).isEqualTo("다시 첫 번째 작성자");
    assertThat(comments.get(4).author().name()).isEqualTo("commenter1");
  }

  @Test
  @DisplayName("대량 댓글 페이징 성능")
  void retrieveComments_largeDataset() {
    // Given
    createTestComments(targetFeed, 100);

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(20)
        .build();

    // When
    long startTime = System.currentTimeMillis();
    FeedCommentData result = feedCommentService.retrieveComment(request);
    long endTime = System.currentTimeMillis();

    // Then
    assertThat(result.data()).hasSize(20);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.totalCount()).isEqualTo(100);

    // 성능 확인 (1초 이내)
    assertThat(endTime - startTime).isLessThan(1000);

    // 데이터 정합성 확인
    assertThat(result.data()).isSortedAccordingTo(
        (a, b) -> a.createdAt().compareTo(b.createdAt()));
  }

  @Test
  @DisplayName("댓글 작성자 프로필 정보 완전성")
  void retrieveComments_completeUserProfile() {
    // Given
    createCommentWithContent(targetFeed, commenter1, "프로필 테스트");

    CommentPaginationRequest request = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .build();

    // When
    FeedCommentData result = feedCommentService.retrieveComment(request);

    // Then
    assertThat(result.data()).hasSize(1);

    FeedCommentDto comment = result.data().get(0);
    assertThat(comment.author()).isNotNull();
    assertThat(comment.author().userId()).isEqualTo(commenter1.getId());
    assertThat(comment.author().name()).isEqualTo("commenter1");
    assertThat(comment.author().profileImageUrl()).isEqualTo("commenter1.jpg");
    assertThat(comment.content()).isEqualTo("프로필 테스트");
    assertThat(comment.feedId()).isEqualTo(targetFeed.getId());
    assertThat(comment.createdAt()).isNotNull();
    assertThat(comment.id()).isNotNull();
  }

  @Test
  @DisplayName("실제 시나리오: 게시글에 댓글 달기")
  void retrieveComments_realWorldScenario() throws InterruptedException {
    // Given - 실제 시나리오처럼 시간차를 두고 댓글 작성
    createCommentWithContent(targetFeed, commenter1, "멋진 코디네요! 👍");
    Thread.sleep(200);

    createCommentWithContent(targetFeed, commenter2, "어디서 구매하셨나요?");
    Thread.sleep(150);

    createCommentWithContent(targetFeed, author, "@commenter2 유니클로에서 샀어요!");
    Thread.sleep(180);

    createCommentWithContent(targetFeed, commenter3, "저도 이런 스타일 좋아해요 ㅎㅎ");
    Thread.sleep(100);

    createCommentWithContent(targetFeed, commenter1, "다음에 같이 쇼핑해요!");

    // When - 첫 번째 페이지 (3개)
    CommentPaginationRequest firstRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .limit(3)
        .build();

    FeedCommentData firstPage = feedCommentService.retrieveComment(firstRequest);


    // Then - 첫 번째 페이지 검증
    assertThat(firstPage.data()).hasSize(3);
    assertThat(firstPage.hasNext()).isTrue();
    assertThat(firstPage.totalCount()).isEqualTo(5);

    // 댓글 내용과 순서 확인
    List<FeedCommentDto> comments = firstPage.data();
    assertThat(comments.get(0).content()).isEqualTo("멋진 코디네요! 👍");
    assertThat(comments.get(1).content()).isEqualTo("어디서 구매하셨나요?");
    assertThat(comments.get(2).content()).isEqualTo("@commenter2 유니클로에서 샀어요!");

    // When - 두 번째 페이지
    CommentPaginationRequest secondRequest = CommentPaginationRequest.builder()
        .feedId(targetFeed.getId())
        .cursor(firstPage.nextCursor())
        .idAfter(firstPage.nextIdAfter())
        .limit(3)
        .build();

    FeedCommentData secondPage = feedCommentService.retrieveComment(secondRequest);

    // Then - 두 번째 페이지 검증
    assertThat(secondPage.data()).hasSize(2);
    assertThat(secondPage.hasNext()).isFalse();
    assertThat(secondPage.totalCount()).isEqualTo(5);

    List<FeedCommentDto> secondComments = secondPage.data();
    assertThat(secondComments.get(0).content()).isEqualTo("저도 이런 스타일 좋아해요 ㅎㅎ");
    assertThat(secondComments.get(1).content()).isEqualTo("다음에 같이 쇼핑해요!");
  }

  // Helper methods
  private List<FeedComment> createTestComments(Feed feed, int count) {
    List<FeedComment> comments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User commenter = switch (i % 3) {
        case 0 -> commenter1;
        case 1 -> commenter2;
        default -> commenter3;
      };
      FeedComment comment = createCommentWithContent(feed, commenter, "Test comment " + i);
      comments.add(comment);
    }
    return comments;
  }

  private List<FeedComment> createTestCommentsWithTimeGap(Feed feed, int count) throws InterruptedException {
    List<FeedComment> comments = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      User commenter = switch (i % 3) {
        case 0 -> commenter1;
        case 1 -> commenter2;
        default -> commenter3;
      };
      FeedComment comment = createCommentWithContent(feed, commenter, "Timed comment " + i);
      comments.add(comment);
      if (i < count - 1) {
        Thread.sleep(50); // 시간 간격
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