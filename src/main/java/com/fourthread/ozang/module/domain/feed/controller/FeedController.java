package com.fourthread.ozang.module.domain.feed.controller;

import static org.springframework.http.HttpStatus.CREATED;

import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.dto.request.FeedCreateRequest;
import com.fourthread.ozang.module.domain.feed.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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

}
