package com.fourthread.ozang.module.domain.feed.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;

@Builder
public record CommentPaginationRequest (

  String cursor,
  UUID idAfter,

  @NotNull
  Integer limit

)
{}