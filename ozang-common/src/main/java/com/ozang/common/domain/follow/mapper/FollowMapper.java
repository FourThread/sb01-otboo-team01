package com.ozang.common.domain.follow.mapper;

import com.ozang.common.domain.follow.dto.FollowDto;
import com.ozang.common.domain.follow.entity.Follow;
import com.ozang.common.domain.user.dto.data.UserSummary;
import com.ozang.common.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class FollowMapper {

    public FollowDto toDto(Follow follow) {
        return new FollowDto(
                follow.getId(),
                toUserSummary(follow.getFollower()),
                toUserSummary(follow.getFollowee())
        );
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(
                user.getId(),
                user.getName(),
                user.getProfile().getProfileImageUrl()
        );
    }
}