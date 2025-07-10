package com.fourthread.ozang.module.domain.feed.repository;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.SortBy;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
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

@org.springframework.context.annotation.Profile("test")
@SpringBootTest
@Transactional
class FeedRepositoryTest {

  @Autowired
  private FeedRepository feedRepository;

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

  private User author1, author2;
  private Weather clearWeather, cloudyWeather;
  private Clothes clothes1;

  @BeforeEach
  void setUp() {
    setupUsers();
    setupWeather();
    setupClothes();
  }

  private void setupUsers() {
    // 사용자1 생성
    author1 = userRepository.save(new User("user1", "user1@example.com", "password"));
    Profile profile1 = new Profile("User One", Gender.MALE, LocalDate.now().minusYears(25),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 3, "profile1.jpg");
    profile1.setUser(author1);
    profileRepository.save(profile1);

    // 사용자2 생성
    author2 = userRepository.save(new User("user2", "user2@example.com", "password"));
    Profile profile2 = new Profile("User Two", Gender.FEMALE, LocalDate.now().minusYears(23),
        new Location(37.5665, 126.9780, 60, 127, List.of("Seoul")), 2, "profile2.jpg");
    profile2.setUser(author2);
    profileRepository.save(profile2);
  }

  private void setupWeather() {
    WeatherAPILocation location = new WeatherAPILocation(
        37.5665, 126.9780, 60, 127, List.of("Seoul", "Gangnam-gu")
    );

    clearWeather = weatherRepository.save(
        Weather.create(LocalDateTime.now().minusHours(1), LocalDateTime.now(), location, SkyStatus.CLEAR)
    );

    cloudyWeather = weatherRepository.save(
        Weather.create(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1), location, SkyStatus.CLOUDY)
    );
  }

  private void setupClothes() {
    clothes1 = clothesRepository.save(Clothes.builder()
        .name("T-shirt")
        .ownerId(author1.getId())
        .imageUrl("tshirt.jpg")
        .type(null)
        .build());

    ClothesAttributeDefinition definition = attributeDefRepository.save(
        new ClothesAttributeDefinition("color", List.of("red", "blue", "black"))
    );
  }

  @Test
  @DisplayName("기본 피드 검색 - 모든 피드 조회")
  void search_allFeeds() {
    // Given
    createTestFeeds(5);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(5);
    assertThat(result).isSortedAccordingTo((a, b) -> b.createdAt().compareTo(a.createdAt()));
  }

  @Test
  @DisplayName("키워드 검색 - 내용으로 피드 필터링")
  void search_withKeyword() {
    // Given
    createFeedWithContent("Today is a beautiful day!", author1, clearWeather);
    createFeedWithContent("Rainy weather makes me sad", author2, cloudyWeather);
    createFeedWithContent("I love sunny days", author1, clearWeather);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .keywordLike("day")
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(feed ->
        feed.content().toLowerCase().contains("day"));
  }

  @Test
  @DisplayName("하늘 상태로 피드 필터링")
  void search_bySkyStatus() {
    // Given
    createFeedWithContent("Clear sky today", author1, clearWeather);
    createFeedWithContent("Cloudy weather", author2, cloudyWeather);
    createFeedWithContent("Another clear day", author1, clearWeather);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .skyStatusEqual(SkyStatus.CLEAR)
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(feed ->
        feed.weather().skyStatus() == SkyStatus.CLEAR);
  }

  @Test
  @DisplayName("작성자로 피드 필터링")
  void search_byAuthor() {
    // Given
    createFeedWithContent("Feed by author1", author1, clearWeather);
    createFeedWithContent("Feed by author2", author2, cloudyWeather);
    createFeedWithContent("Another feed by author1", author1, clearWeather);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .authorIdEqual(author1.getId())
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(2);
    assertThat(result).allMatch(feed ->
        feed.author().userId().equals(author1.getId()));
  }

  @Test
  @DisplayName("ASCENDING 정렬로 피드 조회")
  void search_ascendingOrder() throws InterruptedException {
    // Given
    createTestFeedsWithTimeGap(3);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.ASCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result).isSortedAccordingTo((a, b) -> a.createdAt().compareTo(b.createdAt()));
  }

  @Test
  @DisplayName("좋아요 수로 정렬")
  void search_orderByLikeCount() {
    // Given
    createFeedWithLikes("Low likes", author1, clearWeather, 5);
    createFeedWithLikes("High likes", author2, cloudyWeather, 50);
    createFeedWithLikes("Medium likes", author1, clearWeather, 20);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .limit(10)
        .sortBy(SortBy.likeCount)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(3);
    assertThat(result.get(0).likeCount()).isEqualTo(50);
    assertThat(result.get(1).likeCount()).isEqualTo(20);
    assertThat(result.get(2).likeCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("페이지 제한 테스트")
  void search_withLimit() {
    // Given
    createTestFeeds(10);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .limit(3)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(4); // limit + 1 for hasNext check
  }

  @Test
  @DisplayName("커서 기반 페이징 - DESCENDING")
  void search_cursorPaging_descending() throws InterruptedException {
    // Given
    createTestFeedsWithTimeGap(5);

    // 첫 번째 페이지
    FeedPaginationRequest firstRequest = FeedPaginationRequest.builder()
        .limit(2)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    List<FeedDto> firstPage = feedRepository.search(firstRequest);

    // 두 번째 페이지
    FeedDto lastFeedFromFirstPage = firstPage.get(1); // limit만큼만 사용
    FeedPaginationRequest secondRequest = FeedPaginationRequest.builder()
        .cursor(lastFeedFromFirstPage.createdAt().toString())
        .idAfter(lastFeedFromFirstPage.id().toString())
        .limit(2)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> secondPage = feedRepository.search(secondRequest);

    // Then
    assertThat(firstPage).hasSize(3); // limit + 1
    assertThat(secondPage).hasSize(3); // limit + 1

    // 첫 번째 페이지와 두 번째 페이지의 데이터가 겹치지 않는지 확인
    List<String> firstPageIds = firstPage.stream()
        .limit(2) // 실제 limit만큼만
        .map(feed -> feed.id().toString())
        .toList();
    List<String> secondPageIds = secondPage.stream()
        .limit(2) // 실제 limit만큼만
        .map(feed -> feed.id().toString())
        .toList();

    assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
  }

  @Test
  @DisplayName("커서 기반 페이징 - ASCENDING")
  void search_cursorPaging_ascending() throws InterruptedException {
    // Given
    createTestFeedsWithTimeGap(5);

    // 첫 번째 페이지
    FeedPaginationRequest firstRequest = FeedPaginationRequest.builder()
        .limit(2)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.ASCENDING)
        .build();

    List<FeedDto> firstPage = feedRepository.search(firstRequest);

    // 두 번째 페이지
    FeedDto lastFeedFromFirstPage = firstPage.get(1); // limit만큼만 사용
    FeedPaginationRequest secondRequest = FeedPaginationRequest.builder()
        .cursor(lastFeedFromFirstPage.createdAt().toString())
        .idAfter(lastFeedFromFirstPage.id().toString())
        .limit(2)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.ASCENDING)
        .build();

    // When
    List<FeedDto> secondPage = feedRepository.search(secondRequest);

    // Then
    assertThat(firstPage).hasSize(3); // limit + 1
    assertThat(secondPage).hasSize(3); // limit + 1

    // 첫 번째 페이지와 두 번째 페이지의 데이터가 겹치지 않는지 확인
    List<String> firstPageIds = firstPage.stream()
        .limit(2)
        .map(feed -> feed.id().toString())
        .toList();
    List<String> secondPageIds = secondPage.stream()
        .limit(2)
        .map(feed -> feed.id().toString())
        .toList();

    assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
  }

  @Test
  @DisplayName("복합 조건 검색")
  void search_multipleConditions() {
    // Given
    createFeedWithContent("Beautiful sunny day", author1, clearWeather);
    createFeedWithContent("Beautiful cloudy day", author1, cloudyWeather);
    createFeedWithContent("Ugly sunny day", author2, clearWeather);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .keywordLike("beautiful")
        .authorIdEqual(author1.getId())
        .skyStatusEqual(SkyStatus.CLEAR)
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).content()).contains("Beautiful sunny day");
    assertThat(result.get(0).author().userId()).isEqualTo(author1.getId());
    assertThat(result.get(0).weather().skyStatus()).isEqualTo(SkyStatus.CLEAR);
  }

  @Test
  @DisplayName("전체 피드 개수 조회")
  void feedTotalCount_all() {
    // Given
    createTestFeeds(7);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .build();

    // When
    Long totalCount = feedRepository.feedTotalCount(request);

    // Then
    assertThat(totalCount).isEqualTo(7);
  }

  @Test
  @DisplayName("조건에 맞는 피드 개수 조회")
  void feedTotalCount_withConditions() {
    // Given
    createFeedWithContent("keyword1 content", author1, clearWeather);
    createFeedWithContent("keyword2 content", author2, cloudyWeather);
    createFeedWithContent("keyword1 another", author1, clearWeather);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .keywordLike("keyword1")
        .authorIdEqual(author1.getId())
        .build();

    // When
    Long totalCount = feedRepository.feedTotalCount(request);

    // Then
    assertThat(totalCount).isEqualTo(2);
  }

  @Test
  @DisplayName("limit이 0일 때 기본값 20 적용")
  void search_zeroLimit() {
    // Given
    createTestFeeds(5);

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .limit(0) // 0으로 설정
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .build();

    // When
    List<FeedDto> result = feedRepository.search(request);

    // Then
    assertThat(result).hasSize(5); // 전체 5개 (20 + 1보다 적음)
  }

  // Helper methods
  private List<Feed> createTestFeeds(int count) {
    List<Feed> feeds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Feed feed = createFeedWithContent("Test content " + i,
          i % 2 == 0 ? author1 : author2,
          i % 2 == 0 ? clearWeather : cloudyWeather);
      feeds.add(feed);
    }
    return feeds;
  }

  private List<Feed> createTestFeedsWithTimeGap(int count) throws InterruptedException {
    List<Feed> feeds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Feed feed = createFeedWithContent("Test content " + i, author1, clearWeather);
      feeds.add(feed);
      if (i < count - 1) {
        Thread.sleep(100); // 시간 간격
      }
    }
    return feeds;
  }

  private Feed createFeedWithContent(String content, User author, Weather weather) {
    Feed feed = Feed.builder()
        .author(author)
        .weather(weather)
        .content(content)
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();

    Feed savedFeed = feedRepository.save(feed);
    feedClothesRepository.save(new FeedClothes(clothes1, savedFeed));

    return savedFeed;
  }

  private Feed createFeedWithLikes(String content, User author, Weather weather, int likeCount) {
    Feed feed = Feed.builder()
        .author(author)
        .weather(weather)
        .content(content)
        .likeCount(new AtomicInteger(likeCount))
        .commentCount(new AtomicInteger(0))
        .build();

    Feed savedFeed = feedRepository.save(feed);
    feedClothesRepository.save(new FeedClothes(clothes1, savedFeed));

    return savedFeed;
  }
}