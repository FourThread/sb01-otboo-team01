package com.fourthread.ozang.module.domain.clothes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

//JSON <-> List<String> 변환기
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    try {
      return attribute == null || attribute.isEmpty()
          ? "[]"
          : objectMapper.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("List to JSON 변환 실패", e);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {

    if (dbData == null || dbData.isBlank()) {
      return Collections.emptyList();
    }

    try {
      return objectMapper.readValue(dbData, new TypeReference<List<String>>() {
      });
    } catch (IOException e) {
      throw new IllegalArgumentException("JSON to List 변환 실패", e);
    }
  }
}