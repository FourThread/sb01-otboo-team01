package com.fourthread.ozang.module.domain.feed.service;

import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_LIKE_NOT_FOUND;
import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_NOT_FOUND;

import com.fourthread.ozang.module.common.exception.ErrorCode;
import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesAttributeWithDefDto;
import com.fourthread.ozang.module.domain.clothes.dto.response.OotdDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentData;
import com.fourthread.ozang.module.domain.feed.dto.FeedCommentDto;
import com.fourthread.ozang.module.domain.feed.event.FeedCreatedEvent;
import com.fourthread.ozang.module.domain.feed.dto.FeedData;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedUpdateRequest;
import com.fourthread.ozang.module.domain.feed.elasticsearch.entity.FeedDocument;
import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.AsyncFeedSearchService;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedClothes;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.feed.entity.FeedLike;
import com.fourthread.ozang.module.domain.feed.exception.FeedLikeNotFoundException;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.mapper.FeedMapper;
import com.fourthread.ozang.module.domain.feed.repository.FeedClothesRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.notification.event.FeedCommentedEvent;
import com.fourthread.ozang.module.domain.notification.event.FeedLikedEvent;
import com.fourthread.ozang.module.domain.notification.event.FollowingFeedCreatedEvent;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.exception.UserException;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.WeatherNotFoundException;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FeedService {

  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final FeedClothesRepository feedClothesRepository;
  private final Optional<FeedSearchService> feedSearchService;
  private final Optional<AsyncFeedSearchService> asyncFeedSearchService;
  private final FeedElasticsearchRepository feedElasticsearchRepository;
  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final ClothesRepository clothesRepository;
  private final FeedMapper feedMapper;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * @methodName : register
   * @date : 2025-06-24 오전 11:12
   * @author : wongil
   * @Description: feed 생성
   **/
  public FeedDto registerFeed(FeedCreateRequest request) {
    if (request == null) {
      log.info("null FeedCreateRequest");
      throw new IllegalArgumentException();
    }

    User user = getUser(request.authorId());
    Weather weather = getWeather(request.weatherId());
    List<OotdDto> ootds = getOotds(request);

    Feed feed = Feed.builder()
        .author(user)
        .weather(weather)
        .content(request.content())
        .likeCount(new AtomicInteger(0))
        .commentCount(new AtomicInteger(0))
        .build();

    Feed savedFeed = feedRepository.save(feed);
    saveFeedClothes(ootds, savedFeed);

    List<String> clothesIds = ootds.stream()
        .map(ootd -> ootd.clothesId().toString())
        .toList();
    eventPublisher.publishEvent(new FeedCreatedEvent(savedFeed, clothesIds));
    log.info("피드 저장 완료: feed id={}", feed.getId());

    FeedDto dto = feedMapper.toDto(feed, user, weather, ootds);

    eventPublisher.publishEvent(new FollowingFeedCreatedEvent(dto.author(), feed.getContent()));

    return dto;
  }

  /**
  * @methodName : retrieveFeed
  * @date : 25. 6. 26. 오후 1:17
  * @author : wongil
  * @Description: 피드 목록 조회
  **/
  public CompletableFuture<FeedData>  retrieveFeed(FeedPaginationRequest request, UUID likeByUserId) {
    if (request == null) {
      throw new IllegalArgumentException();
    }

    /// Elasticsearch 사용 가능하고 키워드 검색인 경우에만 Elasticsearch 사용
    return (StringUtils.hasText(request.keywordLike()) && asyncFeedSearchService.isPresent())
        ? asyncFeedSearchService.get().asyncElasticSearch(request, likeByUserId) // 비동기
        : defaultPaging(request, likeByUserId);
  }

  private CompletableFuture<FeedData> defaultPaging(FeedPaginationRequest request, UUID likeByUserId) {
    if (request == null) {
      throw new IllegalArgumentException();
    }

    Integer pageSize = request.limit();
    List<FeedDto> data = feedRepository.search(request, likeByUserId);
    boolean hasNext = data.size() > pageSize;

    List<FeedDto> pagedFeeds = hasNext ? data.subList(0, pageSize) : data;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      FeedDto lastFeed = pagedFeeds.get(pagedFeeds.size() - 1);
      nextCursor = lastFeed.createdAt().toString();
      nextIdAfter = lastFeed.id();
    }

    Long totalCount = feedRepository.feedTotalCount(request);

    FeedData feedData = new FeedData(
        pagedFeeds,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount,
        request.sortBy(),
        request.sortDirection()
    );

    return CompletableFuture.completedFuture(feedData);
  }

  /**
  * @methodName : delete
  * @date : 2025-06-25 오후 1:50
  * @author : wongil
  * @Description: 피드 삭제
  **/
  public void delete(UUID feedId) {
    Feed feed = getFeed(feedId);

    feedClothesRepository.deleteByFeed_Id(feedId);
    feedLikeRepository.deleteAllByFeed_Id(feedId);
    feedRepository.delete(feed);
    feedElasticsearchRepository.deleteById(feedId.toString());

  }

  /**
  * @methodName : update
  * @date : 2025-06-25 오후 2:24
  * @author : wongil
  * @Description: 피드 수정
  **/
  public FeedDto update(UUID feedId, FeedUpdateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException();
    }

    Feed updatedFeed = getFeed(feedId).updateFeed(request.content());

    return feedMapper.toDto(updatedFeed, updatedFeed.getAuthor(), updatedFeed.getWeather(), getOotdsByFeed(updatedFeed));
  }

  /**
  * @methodName : like
  * @date : 2025-06-25 오전 11:35
  * @author : wongil
  * @Description: 피드 좋아요
  **/
  public FeedDto like(UUID feedId, UUID likeByUserId) {

    Feed feed = getFeed(feedId);
    feed.increaseLike();

    User likeByUser = getUser(likeByUserId);

    FeedLike feedLike = new FeedLike(feed, likeByUser);
    feedLikeRepository.save(feedLike);
    reflectFeedLikeOnFeedDocument(feedId, feed);

    eventPublisher.publishEvent(new FeedLikedEvent(feedLike.getId(), feed.getAuthor().getId(), feed.getContent(), likeByUser.getName()));

    return feedMapper.toDto(feed, feed.getAuthor(), feed.getWeather(), getOotdsByFeed(feed));
  }

  /**
  * @methodName : doNotLike
  * @date : 2025-06-25 오전 11:35
  * @author : wongil
  * @Description: 피드 좋아요 취소
  **/
  public FeedDto unLike(UUID feedId, UUID likeByUserId) {

    Feed feed = getFeed(feedId);
    if (!(feed.getLikeCount().get() < 0)) {
      feed.decreaseLike();
    }

    FeedLike feedLike = getFeedLike(feed, likeByUserId);
    feedLikeRepository.delete(feedLike);
    reflectFeedUnLikeOnFeedDocument(feedId, feed);

    return feedMapper.toDto(feed, feed.getAuthor(), feed.getWeather(), getOotdsByFeed(feed));
  }

  /**
  * @methodName : registerComment
  * @date : 2025-06-25 오후 6:00
  * @author : wongil
  * @Description: 피드 댓글 등록
  **/
  public FeedDto postComment(CommentCreateRequest request) {
    if (request == null) {
      throw new IllegalArgumentException();
    }

    Feed feed = getFeed(request.feedId());
    User user = getUser(request.authorId());

    FeedComment comment = FeedComment.builder()
        .feed(feed)
        .author(user)
        .content(request.content())
        .build();

    feedCommentRepository.save(comment);

    eventPublisher.publishEvent(new FeedCommentedEvent(feed.getAuthor().getId(),comment.getAuthor().getName(), comment.getContent()));

    return feedMapper.toDto(feed, user, feed.getWeather(), getOotdsByFeed(feed), comment);
  }

  /**
  * @methodName : retrieveComment
  * @date : 2025-06-30 오전 11:15
  * @author : wongil
  * @Description: 피드 댓글 조회
  **/
  @Transactional(readOnly = true)
  public FeedCommentData retrieveComment(CommentPaginationRequest request) {

    Integer pageSize = request.limit();
    List<FeedCommentDto> data = feedCommentRepository.searchComment(request);
    boolean hasNext = data.size() > pageSize;

    List<FeedCommentDto> feedComments = hasNext ? data.subList(0, pageSize) : data;

    String nextCursor = null;
    UUID nextIdAfter = null;

    if (hasNext) {
      FeedCommentDto lastComment = feedComments.get(feedComments.size() - 1);
      nextCursor = lastComment.createdAt().toString();
      nextIdAfter = lastComment.id();
    }

    Long totalCount = feedCommentRepository.commentTotalCount(request);

    return new FeedCommentData(
        feedComments,
        nextCursor,
        nextIdAfter,
        hasNext,
        totalCount
    );
  }

  private FeedLike getFeedLike(Feed feed, UUID likeByUserId) {
    return feedLikeRepository.findByFeed_IdAndUser_Id(feed.getId(), likeByUserId)
        .orElseThrow(() -> new FeedLikeNotFoundException(FEED_LIKE_NOT_FOUND.getCode(),
            FEED_LIKE_NOT_FOUND.getMessage(),
            new ErrorDetails(this.getClass().getSimpleName(), FEED_LIKE_NOT_FOUND.getMessage())));
  }

  private Feed getFeed(UUID feedId) {

    return feedRepository.findById(feedId)
        .orElseThrow(() -> new FeedNotFoundException(FEED_NOT_FOUND.getCode(), FEED_NOT_FOUND.getMessage(),
            new ErrorDetails(this.getClass().getSimpleName(), FEED_NOT_FOUND.getMessage())));
  }

  private User getUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND, ErrorCode.USER_NOT_FOUND.getMessage(), this.getClass().getSimpleName()));
  }

  private List<OotdDto> getOotds(FeedCreateRequest request) {
    return clothesRepository.findByIdIn(request.clothesIds()).stream()
        .map(clothes ->
            new OotdDto(
                clothes.getId(),
                clothes.getName(),
                clothes.getImageUrl(),
                clothes.getType(),
                getClothesAttributeWithDefDtos(clothes)
            ))
        .toList();
  }

  private List<ClothesAttributeWithDefDto> getClothesAttributeWithDefDtos(Clothes clothes) {
    return clothes.getAttributes().stream()
        .map(attribute ->
            new ClothesAttributeWithDefDto(
                attribute.getDefinition().getId(),
                attribute.getDefinition().getName(),
                attribute.getDefinition().getSelectableValues(),
                attribute.getAttributeValue()
            ))
        .toList();
  }

  private Weather getWeather(UUID weatherId) {
    return weatherRepository.findById(weatherId)
        .orElseThrow(WeatherNotFoundException::new);
  }

  private List<OotdDto> getOotdsByFeed(Feed updatedFeed) {
    return feedClothesRepository.findAll().stream()
        .filter(feedClothes -> feedClothes.getFeed().getId().equals(updatedFeed.getId()))
        .map(feedClothes -> new OotdDto(
            feedClothes.getClothes().getId(),
            feedClothes.getClothes().getName(),
            feedClothes.getClothes().getImageUrl(),
            feedClothes.getClothes().getType(),
            getClothesAttributeWithDefDtos(feedClothes.getClothes())
        ))
        .toList();
  }

  @Transactional
  protected void saveFeedClothes(List<OotdDto> ootds, Feed feed) {
    List<UUID> clothesIds = ootds.stream()
        .map(OotdDto::clothesId)
        .toList();
    List<Clothes> clothes = clothesRepository.findAllByIdIn(clothesIds);
    clothes.forEach(cloth -> {
      FeedClothes feedClothes = new FeedClothes(cloth, feed);
      feedClothesRepository.save(feedClothes);
    });
  }

  private void elasticsearchIfPresentSearchFeed(Feed feed) {
    feedSearchService.ifPresentOrElse(
        service -> {
          service.create(feed);
          log.info("피드가 Elasticsearch에 저장되었습니다: feed id={}", feed.getId());
        },
        () -> log.info("Elasticsearch가 비활성화되어 있어 검색 인덱스에 저장하지 않습니다: feed id={}", feed.getId())
    );
  }

  private FeedDocument getFeedDocument(UUID feedId) {
    return feedElasticsearchRepository.findById(feedId.toString())
        .orElseThrow(() -> new FeedNotFoundException(
            FEED_NOT_FOUND.getCode(),
            FEED_NOT_FOUND.getMessage(),
            new ErrorDetails(
                FeedNotFoundException.class.getSimpleName(),
                FEED_NOT_FOUND.getMessage()
            )
        ));
  }

  private void reflectFeedLikeOnFeedDocument(UUID feedId, Feed feed) {
    FeedDocument feedDocument = getFeedDocument(feedId);
    feedDocument.setLikeCount(feed.getLikeCount().longValue());
    feedDocument.setLikedByMe(true);
    feedElasticsearchRepository.save(feedDocument);
  }

  private void reflectFeedUnLikeOnFeedDocument(UUID feedId, Feed feed) {
    FeedDocument feedDocument = getFeedDocument(feedId);
    feedDocument.setLikeCount(feed.getLikeCount().longValue());
    feedDocument.setLikedByMe(false);
    feedElasticsearchRepository.save(feedDocument);
  }
}
