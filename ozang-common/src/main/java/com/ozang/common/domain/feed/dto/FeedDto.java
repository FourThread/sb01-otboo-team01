package com.ozang.common.domain.feed.dto;

import com.ozang.common.domain.clothes.dto.response.OotdDto;
import com.ozang.common.domain.weather.dto.WeatherSummaryDto;
import com.ozang.common.domain.user.dto.data.UserSummary;
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
