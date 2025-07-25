package com.fourthread.ozang.domain.feed.elasticsearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.support.HttpHeaders;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.fourthread.ozang.domain.feed.elasticsearch.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

  @Value("${elasticsearch.cloud.host}")
  private String host;

  @Value("${elasticsearch.cloud.api-key}")
  private String apiKey;

  @Override
  public ClientConfiguration clientConfiguration() {
    return ClientConfiguration.builder()
        .connectedTo(host)
        .withHeaders(() -> {
          HttpHeaders headers = new HttpHeaders();
          headers.add("Authorization", "ApiKey " + apiKey);
          return headers;
        })
        .build();
  }
}
