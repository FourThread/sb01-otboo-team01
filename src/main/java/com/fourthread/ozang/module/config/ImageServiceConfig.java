package com.fourthread.ozang.module.config;

import com.fourthread.ozang.module.domain.storage.ImageService;
import com.fourthread.ozang.module.domain.storage.S3ImageService;
import com.fourthread.ozang.module.domain.storage.TestImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.services.s3.S3Client;

@Slf4j
@Configuration
public class ImageServiceConfig {

  @Bean(name = "clothesImageService")
  @Profile("!test")  // 테스트 환경이 아닐 때만 S3ImageService 사용
  public ImageService clothesImageService(
      S3Client s3Client,
      @Value("${file.upload.clothes.path}") String clothesPath
  ) {
    return new S3ImageService(s3Client, clothesPath);
  }

  @Bean(name = "profileImageService")
  @Profile("!test")  // 테스트 환경이 아닐 때만 S3ImageService 사용
  public ImageService profileImageService(
      S3Client s3Client,
      @Value("${file.upload.profiles.path}") String profilePath
  ) {
    log.info("[Config] 프로필 이미지 경로: {}", profilePath);
    return new S3ImageService(s3Client, profilePath);
  }
}
