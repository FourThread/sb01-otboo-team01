package com.ozang.web.feed.mapper;

import com.ozang.web.clothes.dto.response.OotdDto;
import com.ozang.web.feed.dto.FeedDto;
import com.ozang.web.feed.entity.Feed;
import com.ozang.web.feed.entity.FeedComment;
import com.ozang.web.feed.repository.FeedLikeRepository;
import com.ozang.web.user.dto.data.UserSummary;
import com.ozang.web.user.entity.User;
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
