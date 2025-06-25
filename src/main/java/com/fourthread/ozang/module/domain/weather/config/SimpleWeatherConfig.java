package com.fourthread.ozang.module.domain.weather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SimpleWeatherConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();  // 기본 설정 사용
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create();  // 기본 설정 사용
    }
}