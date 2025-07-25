package com.fourthread.ozang.module.domain.weather.config;

import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient weatherWebClient(WebClient.Builder builder,
        @Value("${weather.api.base-url}") String baseUrl,
        @Value("${weather.api.key}") String apiKey) {
        // DefaultUriBuilderFactory로 변수만 인코딩 (VALUES_ONLY)
        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);

        // 에러 필터: HTTP 에러 시 본문을 읽어서 예외로 변환
        ExchangeFilterFunction errorFilter = ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .flatMap(body -> Mono.error(new WeatherApiException(
                        "기상청 API 오류: " + body,
                        response.statusCode().toString()
                    )));
            }
            return Mono.just(response);
        });

        return builder
            .uriBuilderFactory(factory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .filter(errorFilter)
            .build();
    }

    @Bean
    public WebClient kakaoWebClient(WebClient.Builder builder,
        @Value("${kakao.api.base-url:https://dapi.kakao.com}") String baseUrl,
        @Value("${kakao.api.key}") String kakaoApiKey) {
        return builder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoApiKey)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}