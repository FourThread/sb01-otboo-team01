package com.fourthread.ozang.module.domain.storage.s3;

import com.fourthread.ozang.module.domain.storage.ProfileStorage;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@ConditionalOnProperty(name = "sb01-otboo-team01.storage.type", havingValue = "s3")
@Component
public class S3ProfileStorage implements ProfileStorage {

  private final String accessKey;
  private final String secretKey;
  private final String region;
  private final String bucket;
  private final long presignedUrlExpirationSeconds;

  public S3ProfileStorage(
      @Value("${sb01-otboo-team01.storage.s3.access-key}") String accessKey,
      @Value("${sb01-otboo-team01.storage.s3.secret-key}") String secretKey,
      @Value("${sb01-otboo-team01.storage.s3.region}") String region,
      @Value("${sb01-otboo-team01.storage.s3.bucket}") String bucket,
      @Value("${sb01-otboo-team01.storage.s3.presigned-url-expiration:3600}") long expirationSeconds
  ) {
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.region = region;
    this.bucket = bucket;
    this.presignedUrlExpirationSeconds = expirationSeconds;
  }

  @Override
  public UUID put(MultipartFile file) {
    UUID key = UUID.randomUUID();
    String s3Key = "otboo/images/" + key;
    try {
      S3Client s3Client = getS3Client();
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(s3Key)
          .contentType(file.getContentType())
          .build();
      s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
      log.info("프로필 이미지 업로드 완료: {}", s3Key);
      return key;
    } catch (IOException | S3Exception e) {
      log.error("S3 업로드 실패: {}", e.getMessage());
      throw new RuntimeException("S3 업로드 실패", e);
    }
  }

  @Override
  public String generatePresignedUrl(UUID key, String contentType) {
    String s3Key = "otboo/images/" + key;
    try (S3Presigner presigner = getS3Presigner()) {
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(s3Key)
          .responseContentType(contentType != null ? contentType : "image/jpeg")
          .build();

      GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofSeconds(presignedUrlExpirationSeconds))
          .getObjectRequest(getRequest)
          .build();

      return presigner.presignGetObject(presignRequest).url().toString();
    }
  }

  private S3Client getS3Client() {
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        ))
        .build();
  }

  private S3Presigner getS3Presigner() {
    return S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKey, secretKey)
        ))
        .build();
  }
}