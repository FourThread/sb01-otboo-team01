package com.fourthread.ozang.module.domain.feed.dto;

import com.fourthread.ozang.module.domain.feed.dto.dummy.OotdDto;
import com.fourthread.ozang.module.domain.feed.dto.dummy.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;

@Builder
public record FeedDto (
    UUID id,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,

    UserSummary author,
    WeatherSummaryDto weather,
    List<OotdDto> ootds,

    String content,
    Long likeCount,
    Integer commentCount,
    Boolean likedByMe
) {

}
