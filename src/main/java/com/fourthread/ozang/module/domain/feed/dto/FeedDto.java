package com.fourthread.ozang.module.domain.feed.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public class FeedDto {

  private UUID id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private UserSummary author;
  private WeatherSummaryDto weather;
  private List<OotdDto> ootds;

  private String contnet;
  private Long likeCount;
  private Integer commentCount;
  private Boolean likedByMe;
}
