package com.fourthread.ozang.module.domain.feed.dto;

import com.fourthread.ozang.module.domain.feed.dto.dummy.SortDirection;
import java.util.List;
import java.util.UUID;

public record FeedData (

    List<FeedDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount,
    String sortBy,
    SortDirection sortDirection

) {
}
