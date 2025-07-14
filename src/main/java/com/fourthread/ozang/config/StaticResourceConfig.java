package com.fourthread.ozang.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${cdn.base-url:}")
    private String cdnBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (!cdnBaseUrl.isEmpty()) {
            // /static/** 요청을 CDN으로 리다이렉트
            registry.addResourceHandler("/static/**")
                .addResourceLocations(cdnBaseUrl + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));
        }

        // Fallback
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)));
    }
}