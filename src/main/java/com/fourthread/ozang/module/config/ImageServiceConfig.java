package com.fourthread.ozang.module.config;

import com.fourthread.ozang.module.domain.storage.ImageService;
import com.fourthread.ozang.module.domain.storage.S3ImageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ImageServiceConfig {

  @Bean(name = "clothesImageService")
  public ImageService clothesImageService(
      S3Client s3Client,
      @Value("${file.upload.clothes.path}") String clothesPath
  ) {
    return new S3ImageService(s3Client, clothesPath);
  }

  @Bean(name = "profileImageService")
  public ImageService profileImageService(
      S3Client s3Client,
      @Value("${file.upload.profiles.path}") String profilePath
  ) {
    return new S3ImageService(s3Client, profilePath);
  }
}
