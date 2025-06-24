package com.fourthread.ozang.module.domain.feed.service;

import com.fourthread.ozang.module.domain.feed.dto.CommentCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.exception.FeedNotFoundException;
import com.fourthread.ozang.module.domain.feed.repository.FeedCommentRepository;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedService {

  private final FeedRepository feedRepository;
  private final FeedCommentRepository feedCommentRepository;
  private final UserRepository userRepository;
  private final WeatherDataRepository weatherDataRepository;

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

    // 유저 정보 찾기

    // 날씨 정보 찾기

    // Feed 객체 생성 저장

    // FeedDto 객체 반환

    return null;
  }

  /**
  * @methodName : like
  * @date : 2025-06-24 오전 11:12
  * @author : wongil
  * @Description: feed 좋아요
  **/
  public FeedDto like(UUID feedId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new FeedNotFoundException());

    feed.increaseLike();

    // FeedDto 객체 반환

    return null;
  }

  /**
  * @methodName : doNotLike
  * @date : 2025-06-24 오전 11:12
  * @author : wongil
  * @Description: feed 좋아요 취소
  **/
  public FeedDto doNotLike(UUID feedId) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new FeedNotFoundException());

    feed.decreaseLike();

    // FeedDto 객체 반환

    return null;
  }

  /**
  * @methodName : registerComment
  * @date : 2025-06-24 오후 3:04
  * @author : wongil
  * @Description: 댓글 생성
  **/
  public FeedDto registerComment(UUID feedId, CommentCreateRequest request) {
    Feed feed = feedRepository.findById(feedId)
        .orElseThrow(() -> new FeedNotFoundException());
    feedCommentRepository.findByAuthor_Id(feedId);

    feed.increaseComment();
  }

  /**
  * @methodName : delete
  * @date : 2025-06-24 오후 3:04
  * @author : wongil
  * @Description: 피드 삭제
  **/
  public FeedDto delete(UUID feedId) {
    if (feedId == null) {
      throw new IllegalArgumentException();
    }

    return feedRepository.deleteById(feedId);
  }

  /**
  * @methodName : find
  * @date : 2025-06-24 오후 3:04
  * @author : wongil
  * @Description: 피드 목록 조회
  **/
  public List<FeedDto> find(FeedPaginationRequest request) {

  }
}
