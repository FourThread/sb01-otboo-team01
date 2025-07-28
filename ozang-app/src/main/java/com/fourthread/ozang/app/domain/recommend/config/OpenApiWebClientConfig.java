package com.fourthread.ozang.app.domain.recommend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenApiWebClientConfig {

  @Value("${openai.api.key}")
  private String openApiKey;

  @Value("${openai.response.url}")
  private String openApiUrl;

  @Bean("recommendationClient")
  public WebClient recommendationClient() {
    return WebClient.builder()
        .baseUrl(openApiUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openApiKey)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build();
  }

}
