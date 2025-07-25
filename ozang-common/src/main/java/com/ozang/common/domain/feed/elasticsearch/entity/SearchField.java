package com.fourthread.ozang.module.domain.feed.elasticsearch.entity;

public enum SearchField {

  CONTENT("content"),
  CHOSUNG("content_chosung"),
  HAN_TO_ENG("content_hantoeng"),
  ENG_TO_HAN("content_engtohan"),
  JAMO("content_jamo"),
  SKY_STATUS("skyStatus"),
  PRECIPITATION("precipitationType"),
  CREATED_AT("createdAt"),
  ID("feedId"),
  DESC("desc"),
  ASC("asc"),
  LIKE_COUNT("likeCount"),
  FUZZ_AUTO("AUTO");

  private final String name;

  SearchField(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
