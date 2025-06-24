package com.fourthread.ozang.module.domain.feed.dto;

import java.util.UUID;
import lombok.Builder;

@Builder
public class CommentCreateRequest {

  private UUID feedId;
  private UUID authorId;
  private String content;
}
