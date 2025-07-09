package com.fourthread.ozang.module.config;

import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {

  @Bean
  @Primary
  public FeedSearchService mockFeedSearchService() {
    return Mockito.mock(FeedSearchService.class);
  }

  @Bean
  @Primary
  public FeedElasticsearchRepository mockFeedElasticsearchRepository() {
    return Mockito.mock(FeedElasticsearchRepository.class);
  }
}