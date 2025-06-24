package com.fourthread.ozang.module.domain.feed.dto;

import com.fourthread.ozang.module.domain.feed.entity.PrecipitationTypeEqual;
import com.fourthread.ozang.module.domain.feed.entity.SkyStatusEqual;
import com.fourthread.ozang.module.domain.feed.entity.SortDirection;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class FeedPaginationRequest {

  private String cursor;
  private String idAfter;

  @NotNull
  private Integer limit;

  @NotNull
  private String sortBy;

  @NotNull
  private SortDirection sortDirection;

  private String keywordLike;
  private SkyStatusEqual skyStatusEqual;
  private PrecipitationTypeEqual precipitationTypeEqual;
  private UUID authorIdEqual;
}
