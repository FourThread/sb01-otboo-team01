package com.ozang.common.domain.feed.dto;

import com.ozang.common.domain.feed.entity.SortBy;
import com.ozang.common.domain.feed.entity.SortDirection;
import java.util.List;
import java.util.UUID;

public record FeedData (

    List<FeedDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    SortBy sortBy,
    SortDirection sortDirection

) {
}
