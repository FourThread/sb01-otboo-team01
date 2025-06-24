package com.fourthread.ozang.module.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public class FeedCreateRequest {

  @NotNull
  private UUID authorId;

  @NotNull
  private UUID weatherId;

  @NotNull
  private List<UUID> clothesIds;

  @NotNull
  private String content;
}
