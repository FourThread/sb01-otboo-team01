package com.fourthread.ozang.module.domain.feed.mapper;

import com.fourthread.ozang.module.domain.feed.dto.FeedDto;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.repository.FeedLikeRepository;
import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.module.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {

  private final FeedLikeRepository feedLikeRepository;

  public FeedDto toDto(Feed feed, User user) {
    return FeedDto.builder()
        .id(feed.getId())
        .createdAt(feed.getCreatedAt())
        .updatedAt(feed.getUpdatedAt())
        .author(new UserSummary(user.getId(), user.getName(), user.getProfile().getProfileImageUrl()))
        .weather(null) // TODO: 실제 날씨 정보로 변경
        .ootds(null) // TODO: 실제 OOTD 정보로 변경
        .content(feed.getContent())
        .likeCount(feed.getLikeCount().longValue())
        .commentCount(feed.getCommentCount().intValue())
        .likedByMe(feedLikeRepository.existsByFeed_IdAndUser_Id(feed.getId(), user.getId()))
        .build();
  }

}
