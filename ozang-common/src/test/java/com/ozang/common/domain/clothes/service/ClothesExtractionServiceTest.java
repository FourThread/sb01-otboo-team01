package com.ozang.common.domain.clothes.service;


import com.ozang.common.exception.ErrorCode;
import com.ozang.common.domain.clothes.exception.ClothesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import static org.assertj.core.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClothesExtractionServiceTest {

  @InjectMocks
  private ClothesExtractionService clothesExtractionService;

  @Test
  @DisplayName("29cm URL을 입력하면 29cm로 인식한다")
  void extractFromUrl_29cmUrl_Success() {
    // given
    String url29cm = "https://www.29cm.co.kr/product/123456";

    // when & then
    assertThatThrownBy(() -> clothesExtractionService.extractFromUrl(url29cm))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("29cm");
  }

  @Test
  @DisplayName("지원하지않는URL_예외발생")
  void extractFromUrl_fail_not_supported() {
    // given
    String url = "https://unknownsite.com/item/123";

    // when & then
    assertThatThrownBy(() -> clothesExtractionService.extractFromUrl(url))
        .isInstanceOf(ClothesException.class)
        .hasMessageContaining(ErrorCode.URL_NOT_SUPPORTED.getMessage());
  }

  @Test
  @DisplayName("null입력_예외발생")
  void extractFromUrl_fail_by_null() {
    assertThatThrownBy(() -> clothesExtractionService.extractFromUrl(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("url is null or empty");
  }
}
