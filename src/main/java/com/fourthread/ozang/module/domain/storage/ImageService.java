package com.fourthread.ozang.module.domain.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ImageService {

    /**
     * 의상 이미지를 업로드
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지 URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 의상 이미지를 삭제
     * @param imageUrl 삭제할 이미지 URL
     */
    void deleteImage(String imageUrl);
}
