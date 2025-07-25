package com.fourthread.ozang.domain.feed.dto;

import com.fourthread.ozang.domain.feed.entity.SortBy;
import com.fourthread.ozang.domain.feed.entity.SortDirection;
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
