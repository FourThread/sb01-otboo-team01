package com.fourthread.ozang.module.domain.feed.service;

import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_LIKE_NOT_FOUND;
import static com.fourthread.ozang.module.common.exception.ErrorCode.FEED_NOT_FOUND;

import com.fourthread.ozang.module.common.exception.ErrorDetails;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
<<<<<<< feat/#8/weather
=======
import com.fourthread.ozang.module.domain.feed.dto.dummy.Weather;
import com.fourthread.ozang.module.domain.feed.dto.dummy.WeatherRepository;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentCreateRequest;
>>>>>>> develop
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedUpdateRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.feed.entity.FeedLike;
import com.fourthread.ozang.module.domain.feed.exception.FeedLikeNotFoundException;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.mapper.FeedMapper;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
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

    User user = getUser(request.authorId()); // TODO: 커스텀 예외로 변경
    Weather weather = weatherRepository.findById(request.weatherId())
        .orElseThrow(() -> new RuntimeException()); // TODO: 커스텀 예외로 변경

    Feed feed = Feed.builder().author(user).weather(weather).content(request.content())
        .likeCount(new AtomicInteger(0)).commentCount(new AtomicInteger(0)).build();
    feedRepository.save(feed);
    log.info("피드 저장 완료: feed id={}", feed.getId());

    return feedMapper.toDto(feed, user);
  }

  /**
  * @methodName : delete
  * @date : 2025-06-25 오후 1:50
  * @author : wongil
  * @Description: 피드 삭제
  **/
  public FeedDto delete(UUID feedId) {
    Feed feed = getFeed(feedId);

    feedLikeRepository.deleteAllByFeed_Id(feedId);
    feedRepository.delete(feed);

    return feedMapper.toDto(feed, feed.getAuthor());
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

    return feedMapper.toDto(updatedFeed, updatedFeed.getAuthor());
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

    return feedMapper.toDto(feed, user);
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

  private User getUser(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException());
  }
}
