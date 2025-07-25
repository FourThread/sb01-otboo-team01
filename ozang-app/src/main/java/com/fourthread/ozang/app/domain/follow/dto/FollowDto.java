package com.fourthread.ozang.app.domain.follow.dto;

import com.fourthread.ozang.app.domain.user.dto.data.UserSummary;

import java.util.UUID;

public record FollowDto(
        UUID id,
        UserSummary follower,
        UserSummary followee
) {}
