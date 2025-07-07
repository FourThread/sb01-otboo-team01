package com.fourthread.ozang.module.domain.clothes.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * 테스트 환경에서 사용되는 ImageService 구현체
 * 실제 파일 업로드 없이 더미 URL을 반환합니다.
 */
@Slf4j
@Service
@Profile("test")  // 테스트 환경에서만 활성화
public class TestImageService implements ImageService {

    private static final String DUMMY_BASE_URL = "https://test-bucket.s3.amazonaws.com/clothes/";

    @Override
    public String uploadClothesImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 테스트용 더미 URL 생성
        String fileName = generateTestFileName(file.getOriginalFilename());
        String dummyUrl = DUMMY_BASE_URL + fileName;

        log.info("Test image upload simulated: {}", dummyUrl);
        return dummyUrl;
    }

    @Override
    public void deleteClothesImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            log.info("Test image deletion simulated: {}", imageUrl);
        }
    }

    /**
     * 테스트용 파일명 생성
     */
    private String generateTestFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return "test-" + UUID.randomUUID().toString() + extension;
    }
}