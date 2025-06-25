package com.fourthread.ozang.module.domain.feed.service;

import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_LIKE_NOT_FOUND;
import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_NOT_FOUND;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.dummy.Weather;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedLike;
import com.fourthread.ozang.module.domain.feed.exception.FeedLikeNotFoundException;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.mapper.FeedMapper;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

  private final FeedRepository feedRepository;
  private final FeedLikeRepository feedLikeRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final UserRepository userRepository;
  private final WeatherRepository weatherRepository;
  private final FeedMapper feedMapper;

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

    User user = userRepository.findById(request.authorId())
        .orElseThrow(() -> new RuntimeException()); // TODO: 커스텀 예외로 변경

    Weather weather = weatherRepository.findById(request.weatherId())
        .orElseThrow(() -> new RuntimeException()); // TODO: 커스텀 예외로 변경

    Feed feed = Feed.builder().author(user).weather(weather).content(request.content())
        .likeCount(new AtomicInteger(0)).commentCount(new AtomicInteger(0)).build();
    feedRepository.save(feed);
    log.info("피드 저장 완료: feed id={}", feed.getId());

    return feedMapper.toDto(feed, user);
  }

  /**
  * @methodName : like
  * @date : 2025-06-25 오전 11:35
  * @author : wongil
  * @Description: 피드 좋아요
  **/
  public FeedDto like(UUID feedId) {

    Feed feed = getFeed(feedId);
    FeedLike feedLike = new FeedLike(feed, feed.getAuthor());
    feedLikeRepository.save(feedLike);

    feed.increaseLike();

    return feedMapper.toDto(feed, feed.getAuthor());
  }

  /**
  * @methodName : doNotLike
  * @date : 2025-06-25 오전 11:35
  * @author : wongil
  * @Description: 피드 좋아요 취소
  **/
  public FeedDto doNotLike(UUID feedId) {

    Feed feed = getFeed(feedId);
    FeedLike feedLike = getFeedLike(feed);

    feedLikeRepository.delete(feedLike);
    feed.decreaseLike();

    return feedMapper.toDto(feed, feed.getAuthor());
  }

  private FeedLike getFeedLike(Feed feed) {
    return feedLikeRepository.findByFeed_IdAndUser_Id(feed.getId(),
            feed.getAuthor().getId())
        .orElseThrow(() -> new FeedLikeNotFoundException(FEED_LIKE_NOT_FOUND.getCode(),
            FEED_LIKE_NOT_FOUND.getMessage(),
            new ErrorDetails(this.getClass().getSimpleName(), FEED_LIKE_NOT_FOUND.getMessage())));
  }

  private Feed getFeed(UUID feedId) {

    return feedRepository.findById(feedId)
        .orElseThrow(() -> new FeedNotFoundException(FEED_NOT_FOUND.getCode(), FEED_NOT_FOUND.getMessage(),
            new ErrorDetails(this.getClass().getSimpleName(), FEED_NOT_FOUND.getMessage())));
  }
}
