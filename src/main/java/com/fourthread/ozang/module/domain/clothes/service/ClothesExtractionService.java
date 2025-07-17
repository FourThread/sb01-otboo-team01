package com.fourthread.ozang.module.domain.clothes.service;

import com.fourthread.ozang.module.domain.clothes.dto.response.ClothesDto;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClothesExtractionService {

  public ClothesDto extractFromUrl(String url) {
    if (url == null || url.isEmpty()) {
      throw new IllegalArgumentException("url is null or empty");
    }

    if (url.contains("musinsa.com")) {
      return extractFromMusinsa(url);
    } else if (url.contains("zigzag.kr")) {
      return extractFromZigzag(url);
    } else {
      throw new UnsupportedOperationException("지원하지 않는 사이트입니다.");
    }
  }


  private ClothesDto extractFromMusinsa(String url) {
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
      log.error("무신사 파싱 실패: {}", e.getMessage(), e);
      throw new RuntimeException("의류 정보를 추출하는 데 실패했습니다.");
    }
}
  private ClothesDto extractFromZigzag(String url) {
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
      log.error("지그재그 파싱 실패: {}", e.getMessage(), e);
      throw new RuntimeException("의류 정보를 추출하는 데 실패했습니다.");
    }
  }

  private ClothesDto extractFrom29cm(String url) {
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
      log.error("29CM 파싱 실패: {}", e.getMessage(), e);
      throw new RuntimeException("의류 정보를 추출하는 데 실패했습니다.");
    }
  }
}
