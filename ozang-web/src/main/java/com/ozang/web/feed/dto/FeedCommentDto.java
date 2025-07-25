package com.ozang.web.feed.dto;

import com.ozang.web.user.dto.data.UserSummary;
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
