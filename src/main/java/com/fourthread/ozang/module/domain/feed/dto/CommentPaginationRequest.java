package com.fourthread.ozang.module.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CommentPaginationRequest {

  private String cursor;
  private UUID idAfter;

  @NotNull
  private Integer limit;
}
