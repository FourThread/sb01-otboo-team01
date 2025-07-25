package com.fourthread.ozang.module.domain.feed.event;

import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
public class FeedSearchEventListener {

  private final Optional<FeedSearchService> feedSearchService;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handleFeedCreated(FeedCreatedEvent event) {
    feedSearchService.ifPresentOrElse(
        service -> {
          service.create(event.getFeed(), event.getClothesIds())
              .thenAccept(document ->
                  log.info("피드가 Elasticsearch에 저장되었습니다: feed id={}", event.getFeed().getId())
              )
              .exceptionally(ex -> {
                log.error("Elasticsearch 저장 실패: feed id={}", event.getFeed().getId(), ex);
                return null;
              });
        },
        () -> log.info("Elasticsearch가 비활성화되어 있어 검색 인덱스에 저장하지 않습니다: feed id={}", event.getFeed().getId())
    );
  }
}
