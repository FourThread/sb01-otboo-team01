package com.ozang.web.follow.mapper;

import com.ozang.web.follow.dto.FollowDto;
import com.ozang.web.follow.entity.Follow;
import com.ozang.web.user.dto.data.UserSummary;
import com.ozang.web.user.entity.User;
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