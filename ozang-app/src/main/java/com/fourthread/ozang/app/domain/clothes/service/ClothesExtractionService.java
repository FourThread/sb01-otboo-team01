package com.fourthread.ozang.app.domain.clothes.service;

import com.fourthread.ozang.app.common.exception.ErrorCode;
import com.fourthread.ozang.app.domain.clothes.dto.response.ClothesDto;
import com.fourthread.ozang.module.domain.clothes.entity.Clothes;
import com.fourthread.ozang.app.domain.clothes.exception.ClothesException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesExtractionService {

  @Transactional(readOnly = true)
  public ClothesDto extractFromUrl(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("url is null or empty");
    }

    String normalizedUrl = url.toLowerCase();

    if (normalizedUrl.contains("musinsa.com")) {
      return extractFromOgMeta(url, "무신사");
    } else if (normalizedUrl.contains("zigzag.kr")) {
      return extractFromOgMeta(url, "지그재그");
    } else if (normalizedUrl.contains("29cm.co.kr")) {
      return extractFromOgMeta(url, "29cm");
    } else {
      throw new ClothesException(ErrorCode.URL_NOT_SUPPORTED, ClothesException.class.toString(), url);
    }
  }

  private ClothesDto extractFromOgMeta(String url, String siteName) {
    try {
      Document doc = Jsoup.connect(url).userAgent("Mozilla").get();

      String title = doc.select("meta[property=og:title]").attr("content");
      String imageUrl = doc.select("meta[property=og:image]").attr("content");

      return ClothesDto.builder()
          .id(null)
          .ownerId(null)
          .name(title)
          .imageUrl(imageUrl)
          .type(null)
          .attributes(null)
          .build();

    } catch (IOException e) {
      log.error("{} 파싱 실패: {}", siteName, e.getMessage(), e);
      throw new RuntimeException(siteName + " 의류 정보를 추출하는 데 실패했습니다.");
    }
  }
}
