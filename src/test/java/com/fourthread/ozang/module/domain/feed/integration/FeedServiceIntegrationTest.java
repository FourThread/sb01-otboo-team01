package com.fourthread.ozang.module.domain.feed.integration;

import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.entity.ClothesAttributeDefinition;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesAttributeDefinitionRepository;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedData;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.SortBy;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import com.fourthread.ozang.module.domain.feed.repository.FeedClothesRepository;
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
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

@Transactional
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "ADMIN_USERNAME=test-admin",
        "ADMIN_EMAIL=test-admin@mail.com",
        "ADMIN_PASSWORD=test-pass",
        "JWT_SECRET=d12d12d21d21d12d2",
        "KAKAO_API_KEY=test",
        "WEATHER_API_KEY=dwqqdd11"
    }
)
public class FeedServiceIntegrationTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private WeatherRepository weatherRepository;

  @Autowired
  private ClothesRepository clothesRepository;

  @Autowired
  private ClothesAttributeDefinitionRepository attributeDefRepository;

  @Autowired
  private FeedClothesRepository feedClothesRepository;


  @Autowired
  private ProfileRepository profileRepository;

  @Autowired
  private FeedRepository feedRepository;

  @Autowired
  private FeedService feedService;

  private User author;
  private Weather weather;
  private Clothes clothes;

  @BeforeEach
  void setup() {
    author = userRepository.save(new User("test-user", "test@example.com", "password"));
    Profile profile = new Profile("user", Gender.ETC, LocalDate.now(),
        new Location(1.0, 1.0, 1, 1, List.of("local")), 1, "url");
    profile.setUser(author);
    profileRepository.save(profile);

    WeatherAPILocation apiLoc = new WeatherAPILocation(
        37.5665, 126.9780, 60, 127,
        List.of("Seoul", "Jongno-gu")
    );
    weather = weatherRepository.save(
        Weather.create(
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now(),
            apiLoc,
            SkyStatus.CLEAR
        )
    );

    clothes = clothesRepository.save(Clothes.builder()
        .name("Hat")
        .ownerId(author.getId())
        .imageUrl("http://example.com/hat.png")
        .type(null)
        .build()
    );

    ClothesAttributeDefinition definition = attributeDefRepository.save(
        new ClothesAttributeDefinition("color", List.of("red", "blue")));

    attributeDefRepository.save(definition);
    clothesRepository.save(clothes);
  }

  @Test
  @DisplayName("피드 목록 조회 - 다음 페이지 없음")
  void retrieveFeed_noNext() {
    LocalDateTime now = LocalDateTime.now();
    Feed feed = Feed.builder()
        .author(author)
        .weather(weather)
        .content("content1")
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();
    feedRepository.save(feed);

    feedClothesRepository.save(new FeedClothes(clothes,feed));

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(10)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData result = feedService.retrieveFeed(request);
    List<FeedDto> dataList = result.data();
    boolean hasNext = result.hasNext();
    String nextCursor = result.nextCursor();
    UUID nextId = result.nextIdAfter();
    long totalCount = result.totalCount();

    assertThat(result).isNotNull();
    assertThat(dataList.size()).isEqualTo(1);
    assertThat(hasNext).isFalse();
    assertThat(nextCursor).isNull();
    assertThat(nextId).isNull();
    assertThat(totalCount).isEqualTo(1);
  }

  @Test
  @DisplayName("피드 목록 조회 - 다음 페이지 있음")
  void retrieveFeed_hasNext() {
    List<Feed> feeds = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      Feed feed = Feed.builder()
          .author(author)
          .weather(weather)
          .content("content" + i)
          .likeCount(new AtomicInteger(0))
          .commentCount(new AtomicInteger(0))
          .build();
      feeds.add(feedRepository.save(feed));

      feedClothesRepository.save(new FeedClothes(clothes, feed));
    }

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(4)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData result = feedService.retrieveFeed(request);
    List<FeedDto> dataList = result.data();
    boolean hasNext = result.hasNext();
    String nextCursor = result.nextCursor();
    UUID nextId = result.nextIdAfter();
    long totalCount = result.totalCount();

    assertThat(result).isNotNull();
    assertThat(dataList.size()).isEqualTo(4);
    assertThat(hasNext).isTrue();
    assertThat(nextCursor).isNotNull();
    assertThat(nextId).isNotNull();
    assertThat(totalCount).isEqualTo(5);
  }

  @Test
  @DisplayName("피드 목록 조회 - 커서 기반 페이징")
  void retrieveFeed_cursorPaging() throws InterruptedException {

    List<Feed> feeds = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      Feed feed = Feed.builder()
          .author(author)
          .weather(weather)
          .content("content" + i)
          .likeCount(new AtomicInteger(0))
          .commentCount(new AtomicInteger(0))
          .build();

      feeds.add(feedRepository.save(feed));
      feedClothesRepository.save(new FeedClothes(clothes, feed));

      Thread.sleep(100);
    }

    FeedPaginationRequest firstRequest = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(3)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData firstResult = feedService.retrieveFeed(firstRequest);

    for (FeedDto feedDto : firstResult.data()) {
      System.out.println("createdAt = " + feedDto.createdAt());
    }
    System.out.println(firstResult.nextCursor());
    System.out.println(firstResult.nextIdAfter());
    System.out.println(firstResult.hasNext());
    System.out.println(firstResult.totalCount());

    assertThat(firstResult.hasNext()).isTrue();

    FeedPaginationRequest secondRequest = FeedPaginationRequest.builder()
        .cursor(firstResult.nextCursor())
        .idAfter(firstResult.nextIdAfter().toString())
        .limit(3)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData secondResult = feedService.retrieveFeed(secondRequest);
    for (FeedDto feedDto : secondResult.data()) {
      System.out.println("createdAt = " + feedDto.createdAt());
    }
    System.out.println(secondResult.nextCursor());
    System.out.println(secondResult.nextIdAfter());
    System.out.println(secondResult.hasNext());
    System.out.println(secondResult.totalCount());

    assertThat(secondResult).isNotNull();
    assertThat(secondResult.hasNext()).isTrue();

    List<UUID> firstPageIds = firstResult.data().stream()
        .map(FeedDto::id)
        .toList();
    List<UUID> secondPageIds = secondResult.data().stream()
        .map(FeedDto::id)
        .toList();

    assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
  }


  @Test
  @DisplayName("피드 목록 조회 - 정확히 limit 개수만큼 있는 경우")
  void retrieveFeed_exactLimit() {
    int feedCount = 3;
    for (int i = 0; i < feedCount; i++) {
      Feed feed = Feed.builder()
          .author(author)
          .weather(weather)
          .content("content" + i)
          .likeCount(new AtomicInteger(0))
          .commentCount(new AtomicInteger(0))
          .build();
      feedRepository.save(feed);
      feedClothesRepository.save(new FeedClothes(clothes, feed));
    }

    FeedPaginationRequest request = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(3)
        .sortBy(SortBy.createdAt)
        .sortDirection(SortDirection.DESCENDING)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData result = feedService.retrieveFeed(request);

    assertThat(result).isNotNull();
    assertThat(result.data().size()).isEqualTo(3);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextIdAfter()).isNull();
    assertThat(result.totalCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("피드 목록 조회 - 커서 기반 페이징 디버깅")
  void retrieveFeed_cursorPaging_debug() throws InterruptedException {
    List<Feed> createdFeeds = new ArrayList<>();

    // 피드 생성
    for (int i = 0; i < 10; i++) {
      Feed feed = Feed.builder()
          .author(author)
          .weather(weather)
          .content("content" + i)
          .likeCount(new AtomicInteger(0))
          .commentCount(new AtomicInteger(0))
          .build();
      createdFeeds.add(feedRepository.save(feed));
      feedClothesRepository.save(new FeedClothes(clothes, feed));

      if (i < 9) Thread.sleep(100);
    }

    // 실제 저장된 데이터 확인
    System.out.println("=== 전체 피드 데이터 (DB에서 조회) ===");
    List<Feed> allFeeds = feedRepository.findAll();
    allFeeds.stream()
        .sorted(Comparator.comparing(Feed::getCreatedAt))
        .forEach(feed -> {
          System.out.println("ID: " + feed.getId() +
              ", Content: " + feed.getContent() +
              ", CreatedAt: " + feed.getCreatedAt());
        });

    System.out.println("총 피드 개수: " + allFeeds.size());

    // ASCENDING 테스트
    System.out.println("\n=== ASCENDING 테스트 ===");
    testPagination(SortDirection.ASCENDING);

    // DESCENDING 테스트
    System.out.println("\n=== DESCENDING 테스트 ===");
    testPagination(SortDirection.DESCENDING);
  }

  private void testPagination(SortDirection direction) {
    // 첫 번째 페이지
    FeedPaginationRequest firstRequest = FeedPaginationRequest.builder()
        .cursor(null)
        .idAfter(null)
        .limit(3)
        .sortBy(SortBy.createdAt)
        .sortDirection(direction)
        .keywordLike(null)
        .skyStatusEqual(null)
        .precipitationTypeEqual(null)
        .authorIdEqual(null)
        .build();

    FeedData firstResult = feedService.retrieveFeed(firstRequest);

    System.out.println("첫 번째 페이지 결과:");
    System.out.println("데이터 개수: " + firstResult.data().size());
    System.out.println("hasNext: " + firstResult.hasNext());
    System.out.println("nextCursor: " + firstResult.nextCursor());
    System.out.println("nextIdAfter: " + firstResult.nextIdAfter());
    System.out.println("totalCount: " + firstResult.totalCount());

    firstResult.data().forEach(feedDto -> {
      System.out.println("  - ID: " + feedDto.id() +
          ", Content: " + feedDto.content() +
          ", CreatedAt: " + feedDto.createdAt());
    });

    if (firstResult.hasNext()) {
      // 두 번째 페이지
      FeedPaginationRequest secondRequest = FeedPaginationRequest.builder()
          .cursor(firstResult.nextCursor())
          .idAfter(firstResult.nextIdAfter().toString())
          .limit(3)
          .sortBy(SortBy.createdAt)
          .sortDirection(direction)
          .keywordLike(null)
          .skyStatusEqual(null)
          .precipitationTypeEqual(null)
          .authorIdEqual(null)
          .build();

      FeedData secondResult = feedService.retrieveFeed(secondRequest);

      System.out.println("두 번째 페이지 결과:");
      System.out.println("데이터 개수: " + secondResult.data().size());
      System.out.println("hasNext: " + secondResult.hasNext());

      secondResult.data().forEach(feedDto -> {
        System.out.println("  - ID: " + feedDto.id() +
            ", Content: " + feedDto.content() +
            ", CreatedAt: " + feedDto.createdAt());
      });

      // 커서 조건 확인
      System.out.println("\n커서 조건 확인:");
      System.out.println("사용된 cursor: " + secondRequest.cursor());
      System.out.println("사용된 idAfter: " + secondRequest.idAfter());
    }
  }
}


