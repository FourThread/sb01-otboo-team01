package com.fourthread.ozang.module.domain.config;

import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {

  @Bean
  public FeedElasticsearchRepository feedElasticsearchRepository() {
    return Mockito.mock(FeedElasticsearchRepository.class);
  }

  @Bean
  public FeedSearchService feedSearchService() {
    return Mockito.mock(FeedSearchService.class);
  }
}
