package com.ozang.common.domain.feed.dto;

import com.ozang.common.domain.user.dto.data.UserSummary;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedCommentDto (

    UUID id,
    LocalDateTime createdAt,
    UUID feedId,
    UserSummary author,
    String content

) {

}
