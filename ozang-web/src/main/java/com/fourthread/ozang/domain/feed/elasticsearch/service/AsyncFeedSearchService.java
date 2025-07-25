package com.fourthread.ozang.domain.feed.elasticsearch.service;

import com.fourthread.ozang.domain.feed.dto.FeedData;
import com.fourthread.ozang.domain.feed.dto.request.FeedPaginationRequest;
import com.fourthread.ozang.domain.feed.elasticsearch.service.FeedSearchService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class AsyncFeedSearchService {

  private final FeedSearchService feedSearchService;

  @Transactional(readOnly = true)
  @Async("feedSearchExecutor")
  public CompletableFuture<FeedData> asyncElasticSearch(FeedPaginationRequest request, UUID likeByUserId) {
    log.info("feed 비동기 검색 시작: {}", request);

    try {
      FeedData feedData = feedSearchService.elasticSearch(request, likeByUserId);
      log.info("feed 비동기 검색 성공: {}", feedData);
      return CompletableFuture.completedFuture(feedData);
    } catch (Exception e) {
      log.warn("feed 비동기 검색 실패", e.getCause());
      return CompletableFuture.failedFuture(e);
    }
  }
}
