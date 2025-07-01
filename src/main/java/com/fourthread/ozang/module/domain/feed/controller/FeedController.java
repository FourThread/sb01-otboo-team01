package com.fourthread.ozang.module.domain.feed.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import com.fourthread.ozang.module.domain.feed.dto.FeedCommentData;
import com.fourthread.ozang.module.domain.feed.dto.FeedData;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.CommentPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedUpdateRequest;
import com.fourthread.ozang.module.domain.feed.service.FeedService;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

  private final FeedService feedService;

  /**
   * @methodName : create
   * @date : 2025-06-24 오전 10:38
   * @author : wongil
   * @Description: 피드 등록
   **/
  @ResponseStatus(CREATED)
  @PostMapping
  public FeedDto createFeed(@Validated @RequestBody FeedCreateRequest request) {
    return feedService.registerFeed(request);
  }

  /**
  * @methodName : findAllFeed
  * @date : 2025-06-26 오후 1:57
  * @author : wongil
  * @Description: 피드 조회
  **/
  @GetMapping
  public FeedData findAllFeed(@Validated @ModelAttribute FeedPaginationRequest request) {
    return feedService.retrieveFeed(request);
  }

  /**
   * @methodName : feedDelete
   * @date : 2025-06-24 오전 11:46
   * @author : wongil
   * @Description: 피드 삭제
   **/
  @ResponseStatus(NO_CONTENT)
  @DeleteMapping("/{feedId}")
  public void deleteFeed(@PathVariable @NotNull UUID feedId) {
    feedService.delete(feedId);
  }

  /**
  * @methodName : updateFeed
  * @date : 2025-06-25 오후 2:23
  * @author : wongil
  * @Description: 피드 수정
  **/
  @PatchMapping("/{feedId}")
  public FeedDto updateFeed(@PathVariable @NotNull UUID feedId,
      @Validated @RequestBody FeedUpdateRequest request) {
    return feedService.update(feedId, request);
  }

  /**
   * @methodName : like
   * @date : 2025-06-24 오전 11:00
   * @author : wongil
   * @Description: 피드 좋아요
   **/
  @PostMapping("/{feedId}/like")
  public FeedDto like(@PathVariable @NotNull UUID feedId) {
    return feedService.like(feedId);
  }

  /**
  * @methodName : undoLike
  * @date : 2025-06-24 오전 11:14
  * @author : wongil
  * @Description: 피드 좋아요 취소
  **/
  @DeleteMapping("/{feedId}/like")
  public FeedDto undoLike(@PathVariable UUID feedId) {
    return feedService.unLike(feedId);
  }

  /**
  * @methodName : publishComment
  * @date : 2025-06-25 오후 5:55
  * @author : wongil
  * @Description: 피드 댓글 등록
  **/
  @PostMapping("/{feedId}/comments")
  public FeedDto publishComment(@PathVariable UUID feedId,
      @Validated @RequestBody CommentCreateRequest request) {

    return feedService.postComment(request);
  }

  /**
  * @methodName : searchComment
  * @date : 2025-06-30 오전 11:24
  * @author : wongil
  * @Description: 피드 댓글 조회
  **/
  @GetMapping("/{feedId}/comments")
  public FeedCommentData searchComment(@Validated @ModelAttribute CommentPaginationRequest request) {

    return feedService.retrieveComment(request);
  }

}
