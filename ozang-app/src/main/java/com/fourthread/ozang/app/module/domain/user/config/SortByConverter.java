package com.fourthread.ozang.module.domain.user.config;

import com.fourthread.ozang.module.domain.user.dto.type.SortBy;
import org.springframework.stereotype.Component;
import org.springframework.core.convert.converter.Converter;

@Component
public class SortByConverter implements Converter<String, SortBy> {
  @Override
  public SortBy convert(String source) {
    return SortBy.valueOf(source.toUpperCase());
  }
}
