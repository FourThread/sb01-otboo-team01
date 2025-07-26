package com.fourthread.ozang.app.domain.follow.mapper;

import com.fourthread.ozang.core.domain.follow.dto.FollowDto;
import com.fourthread.ozang.core.domain.follow.entity.Follow;
import com.fourthread.ozang.core.domain.user.dto.data.UserSummary;
import com.fourthread.ozang.core.domain.user.entity.User;
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