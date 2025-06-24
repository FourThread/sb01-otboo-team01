package com.fourthread.ozang.module.domain.storage;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileStorage {

  UUID put(MultipartFile file);

  String generatePresignedUrl(UUID key, String contentType);

}
