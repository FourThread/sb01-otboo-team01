package com.fourthread.ozang.app.domain.feed.dto;

import com.fourthread.ozang.app.domain.user.dto.data.UserSummary;
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
