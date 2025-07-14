package com.fourthread.ozang.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.cdn.base-url:}")
    private String cdnBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("CDN Configuration: " + cdnBaseUrl);

        if (!cdnBaseUrl.isEmpty()) {
            // CDN을 통한 정적 파일 서빙
            registry.addResourceHandler("/static/**")
                .addResourceLocations(cdnBaseUrl + "/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));

            registry.addResourceHandler("/assets/**")
                .addResourceLocations(cdnBaseUrl + "/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));

            System.out.println("CDN handlers configured: " + cdnBaseUrl);
        }


        registry.addResourceHandler("/static/**")
            .addResourceLocations("classpath:/static/")
            .setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)));

        registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/")
            .setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)));

        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/favicon.ico")
            .setCacheControl(CacheControl.maxAge(Duration.ofDays(1)));

        System.out.println("Fallback handlers configured");
    }
}