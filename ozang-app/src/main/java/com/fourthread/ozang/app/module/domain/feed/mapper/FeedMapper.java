package com.fourthread.ozang.module.domain.feed.mapper;

import com.fourthread.ozang.module.domain.clothes.dto.response.OotdDto;
import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.entity.FeedComment;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.entity.User;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
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
