package com.fourthread.ozang.module.domain.storage;

import org.springframework.web.multipart.MultipartFile;

public interface ProfileStorage {

  String saveFile(MultipartFile file);

}
