package com.fourthread.ozang.module.domain.config;

import com.fourthread.ozang.module.domain.feed.elasticsearch.repository.FeedElasticsearchRepository;
import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import java.util.Optional;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Configuration
@Profile("test")
@EnableAutoConfiguration(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    ReactiveElasticsearchRepositoriesAutoConfiguration.class,
    ElasticsearchClientAutoConfiguration.class
})
public class TestConfig {
  @Bean
  @Primary
  @ConditionalOnMissingBean
  public FeedElasticsearchRepository mockFeedElasticsearchRepository() {
    return Mockito.mock(FeedElasticsearchRepository.class);
  }

  @Bean
  @Primary
  @ConditionalOnMissingBean
  public FeedSearchService mockFeedSearchService() {
    return Mockito.mock(FeedSearchService.class);
  }

  @Bean
  @Primary
  @ConditionalOnMissingBean
  public ElasticsearchOperations mockElasticsearchOperations() {
    return Mockito.mock(ElasticsearchOperations.class);
  }

  // 테스트 환경에서 FeedSearchService Optional 제공
  @Bean
  @Primary
  public Optional<FeedSearchService> feedSearchServiceOptional(FeedSearchService feedSearchService) {
    return Optional.of(feedSearchService);
  }
}
