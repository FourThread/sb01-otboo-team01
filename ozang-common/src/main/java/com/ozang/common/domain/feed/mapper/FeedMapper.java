package com.ozang.common.domain.feed.mapper;

import com.ozang.common.domain.clothes.dto.response.OotdDto;
import com.ozang.common.domain.feed.dto.FeedDto;
import com.ozang.common.domain.feed.entity.Feed;
import com.ozang.common.domain.feed.entity.FeedComment;
import com.ozang.common.domain.feed.repository.FeedLikeRepository;
import com.ozang.common.domain.user.dto.data.UserSummary;
import com.ozang.common.domain.user.entity.User;
import com.ozang.common.domain.weather.dto.WeatherSummaryDto;
import com.ozang.common.domain.weather.entity.Weather;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {

  private final FeedLikeRepository feedLikeRepository;

  public FeedDto toDto(Feed feed, User user, Weather weather, List<OotdDto> ootds) {
    return FeedDto.builder()
        .id(feed.getId())
        .createdAt(feed.getCreatedAt())
        .updatedAt(feed.getUpdatedAt())
        .author(new UserSummary(user.getId(), user.getName(), user.getProfile().getProfileImageUrl()))
        .weather(new WeatherSummaryDto(weather.getId(), weather.getSkyStatus(), weather.getPrecipitation(), weather.getTemperature()))
        .ootds(ootds)
        .content(feed.getContent())
        .likeCount(feed.getLikeCount().longValue())
        .commentCount(feed.getCommentCount().intValue())
        .likedByMe(feedLikeRepository.existsByFeed_IdAndUser_Id(feed.getId(), user.getId()))
        .build();
  }

  public FeedDto toDto(Feed feed, User user, Weather weather, List<OotdDto> ootds, FeedComment feedComment) {
    return FeedDto.builder()
        .id(feed.getId())
        .createdAt(feed.getCreatedAt())
        .updatedAt(feed.getUpdatedAt())
        .author(new UserSummary(user.getId(), user.getName(), user.getProfile().getProfileImageUrl()))
        .weather(new WeatherSummaryDto(weather.getId(), weather.getSkyStatus(), weather.getPrecipitation(), weather.getTemperature()))
        .ootds(ootds)
        .content(feedComment.getContent())
        .likeCount(feed.getLikeCount().longValue())
        .commentCount(feed.getCommentCount().intValue())
        .likedByMe(feedLikeRepository.existsByFeed_IdAndUser_Id(feed.getId(), user.getId()))
        .build();
  }

}
