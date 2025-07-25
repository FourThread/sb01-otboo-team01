package com.ozang.web.feed.dto;

import java.util.List;
import java.util.UUID;

public record FeedCommentData(

    List<FeedCommentDto> data,
    String nextCursor,
    UUID nextIdAfter,
    boolean hasNext,
    Long totalCount

) {
}
