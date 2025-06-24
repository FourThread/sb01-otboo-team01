package com.fourthread.ozang.module.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public class CommentCreateRequest {

  @NotNull
  private UUID feedId;

  @NotNull
  private UUID authorId;

  @NotNull
  private String content;
}
