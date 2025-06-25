package com.fourthread.ozang.module.domain.storage.s3;

import com.amazonaws.services.s3.AmazonS3;;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fourthread.ozang.module.domain.storage.ProfileStorage;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@ConditionalOnProperty(name = "sb01-otboo-team01.storage.type", havingValue = "s3")
@Component
@RequiredArgsConstructor
public class S3ProfileStorage implements ProfileStorage {

  private final AmazonS3 amazonS3;

  @Value("${sb01-otboo-team01.storage.s3.bucket}")
  private String bucket;

  @Override
  public String saveFile(MultipartFile multipartFile) {
    try {
      String originalFilename = multipartFile.getOriginalFilename();
      String extension = Objects.requireNonNull(originalFilename)
          .substring(originalFilename.lastIndexOf('.') + 1);

      String s3Key = "Profile/" + UUID.randomUUID() + "." + extension;

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(multipartFile.getSize());
      metadata.setContentType(multipartFile.getContentType());

      amazonS3.putObject(bucket, s3Key, multipartFile.getInputStream(), metadata);
      return amazonS3.getUrl(bucket, s3Key).toString();

    } catch (IOException e) {
      log.error("프로필 파일 업로드 중 IOException 발생", e);
      throw new IllegalArgumentException("프로필 파일 업로드에 실패했습니다.", e);
    }
  }
}