package com.fourthread.ozang.module.domain.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
public class TestImageConfig {

  @Bean(name = "clothesImageService")
  @Profile("test")  // 테스트 환경에서만 TestImageService 사용
  public ImageService testClothesImageService() {
    return new TestImageService();
  }

  @Bean(name = "profileImageService")
  @Profile("test")  // 테스트 환경에서만 TestImageService 사용
  public ImageService testProfileImageService() {
    return new TestImageService();
  }

}
