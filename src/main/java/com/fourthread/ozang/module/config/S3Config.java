package com.fourthread.ozang.module.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

  @Value("${sb01-otboo-team01.storage.s3.access-key}")
  private String accessKey;

  @Value("${sb01-otboo-team01.storage.s3.secret-key}")
  private String secretKey;

  @Value("${sb01-otboo-team01.storage.s3.region}")
  private String region;

  @Bean
  public AmazonS3 amazonS3Client() {
    BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

    return AmazonS3ClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(credentials))
        .build();
  }

}
